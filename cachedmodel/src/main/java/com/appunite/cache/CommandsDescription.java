/*
 * Copyright (C) 2014 Jacek Marchwicki <jacek.marchwicki@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.appunite.cache;

import com.google.common.base.Function;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

public class CommandsDescription {

    private static class CommandToExecute<T, K> {
        public static final Comparator<? super CommandToExecute<?, ?>> SORT_VERSION_ASC = new Comparator<CommandToExecute<?, ?>>() {
            @Override
            public int compare(CommandToExecute<?, ?> o1, CommandToExecute<?, ?> o2) {
                return o1.version = o2.version;
            }
        };
        public int version;
        @Nonnull
        public CommandName<T, K> commandName;
        @Nullable
        public K parameter;

        public CommandToExecute(int version,
                                @Nonnull CommandName<T, K> commandName,
                                @Nullable K parameter) {
            this.version = version;
            this.commandName = commandName;
            this.parameter = parameter;
        }

        @Nonnull
        public static <T> Predicate<? super CommandToExecute<?, ?>> matchingCommandsNames(
                @Nonnull final List<CommandName<T, ?>> commandsNames) {
            checkNotNull(commandsNames);
            return new Predicate<CommandToExecute<?, ?>>() {
                @Override
                public boolean apply(@Nullable CommandToExecute<?, ?> input) {
                    if (input == null) {
                        return false;
                    }
                    for (CommandName<T, ?> commandName : commandsNames) {
                        if (Objects.equal(commandName, input.commandName)) {
                            return true;
                        }
                    }
                    return false;
                }
            };
        }

        @Nonnull
        public static Predicate<? super CommandToExecute<?, ?>> matchingVersionGraterOrEqualTo(final int version) {
            return new Predicate<CommandToExecute<?, ?>>() {
                @Override
                public boolean apply(@Nullable CommandToExecute<?, ?> input) {
                    return input != null && input.version >= version;
                }
            };
        }

        @Nonnull
        public static <T> Predicate<? super CommandToExecute<?, ?>> matchingCacheElement(
                @Nonnull final CacheElement<T> cacheElement) {
            checkNotNull(cacheElement);
            return Predicates.and(matchingVersionGraterOrEqualTo(cacheElement.version), matchingCommandsNames(cacheElement.commands));
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("version", version)
                    .add("commandName", commandName)
                    .add("parameter", parameter)
                    .toString();
        }
    }

    private static class CacheElement<T> {

        private static <X> Function<CacheElement<X>, X> toObject() {
            return new Function<CacheElement<X>, X>() {
                @Nullable
                @Override
                public X apply(@Nullable CacheElement<X> input) {
                    return input == null ? null : input.object;
                }
            };
        }

        public int version;
        @Nonnull
        public T object;
        @Nonnull
        public List<CommandName<T, ?>> commands;

        public CacheElement(int version,
                            @Nonnull T object,
                            @Nonnull List<CommandName<T, ?>> commands) {
            this.version = version;
            this.object = checkNotNull(object);
            this.commands = checkNotNull(commands);
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("version", version)
                    .add("object", object)
                    .add("commands", commands)
                    .toString();
        }
    }

    private static class CacheHolder<T> {
        @Nonnull
        Optional<CacheElement<T>> object;
        @Nonnull
        List<CacheInvalidationListener<T>> references = Lists.newArrayList();

        private CacheHolder(@Nonnull Optional<CacheElement<T>> object) {
            this.object = object;
        }

        public void onChanged(T object) {
            final ImmutableList<CacheInvalidationListener<T>> copy = ImmutableList.copyOf(references);
            for (CacheInvalidationListener<T> reference : copy) {
                reference.onChanged(object);
            }
        }

        public void onInvalidated() {
            final ImmutableList<CacheInvalidationListener<T>> copy = ImmutableList.copyOf(references);
            for (CacheInvalidationListener<T> reference : copy) {
                reference.onInvalidated();
            }
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("object", object)
                    .add("references", references)
                    .toString();
        }
    }

    @Nonnull
    private final HashMap<CommandName<?, ?>, Command<?,?>> mCommands = Maps.newHashMap();
    @Nonnull
    private final List<CommandToExecute<?, ?>> mToExecute = Lists.newArrayList();
    @Nonnull
    private Cache<Object, CacheElement<?>> mCache = CacheBuilder.newBuilder()
            .maximumSize(20)
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .build();
    @Nonnull
    private final Map<CacheKey<?>, CacheHolder<?>> mHardReferenceStore = Maps.newHashMap();

    public CommandsDescription() {
    }

    public <T, K> void addCommand(@Nonnull CommandName<T, K> commandName, Command<T, K> command) {
        checkNotNull(command);
        mCommands.put(commandName, command);
    }

    public <T, K> void invalidate(@Nonnull CommandName<T, K> commandName, @Nullable K parameter) {
        checkNotNull(commandName);
        final Command<?, ?> command = mCommands.get(commandName);
        checkState(command != null, "You did not defined command: " + commandName);

        mToExecute.add(new CommandToExecute<>(mVersion++, commandName, parameter));

        for (Map.Entry<CacheKey<?>, CacheHolder<?>> cacheKeyCacheHolderEntry : mHardReferenceStore.entrySet()) {
            final CacheHolder<?> cacheHolder = cacheKeyCacheHolderEntry.getValue();
            if (cacheHolder.object.isPresent()) {
                final CacheElement<?> cacheElement = cacheHolder.object.get();
                //noinspection SuspiciousMethodCalls
                if (cacheElement.commands.contains(commandName)) {
                    //noinspection unchecked
                    final CacheKey<T> key = (CacheKey<T>) cacheKeyCacheHolderEntry.getKey();
                    //noinspection unchecked
                    recalculate(key, (CacheElement<T>) cacheElement);
                }
            }
        }
    }

    public <T> void register(@Nonnull CacheKey<T> cacheKey,
                             @Nonnull CacheInvalidationListener<T> listener) {
        checkNotNull(cacheKey);
        checkNotNull(listener);

        final CacheHolder<T> cacheHolder = getCacheHolderOrCreate(cacheKey);
        cacheHolder.references.add(listener);
    }

    @Nonnull
    @SuppressWarnings("unchecked")
    private <T, K> Command<T, K> getCommand(@Nonnull CommandToExecute<T, K> commandToExecute) {
        checkNotNull(commandToExecute);
        return checkNotNull((Command<T, K>) mCommands.get(commandToExecute.commandName));
    }

    @Nullable
    @SuppressWarnings("unchecked")
    private <T> CacheHolder<T> getCacheHolder(@Nonnull CacheKey<T> cacheKey) {
        checkNotNull(cacheKey);
        return (CacheHolder<T>) mHardReferenceStore.get(cacheKey);
    }

    @Nonnull
    private <T, K> Command.UpdateResult<T> executeCommand(@Nonnull T object,
                                                          @Nonnull CommandToExecute<T, K> commandToExecute) {
        checkNotNull(object);
        checkNotNull(commandToExecute);
        final Command<T, K> command = getCommand(commandToExecute);
        return command.apply(commandToExecute.parameter, object);
    }

    @Nonnull
    private <T> CacheHolder<T> getCacheHolderOrCreate(@Nonnull CacheKey<T> cacheKey) {
        checkNotNull(cacheKey);
        CacheHolder<T> cacheHolder = getCacheHolder(cacheKey);
        if (cacheHolder == null) {
            final Optional<CacheElement<T>> cacheIfPresent = getCacheElementIfPresent(cacheKey);
            cacheHolder = new CacheHolder<>(cacheIfPresent);
            mHardReferenceStore.put(cacheKey, cacheHolder);
        }
        return cacheHolder;
    }

    @Nonnull
    public <T> Optional<CacheElement<T>> getCacheElementIfPresent(CacheKey<T> cacheKey) {
        checkNotNull(cacheKey);
        final Optional<CacheElement<T>> optCacheElement;
        final CacheHolder<T> cacheHolder = getCacheHolder(cacheKey);
        if (cacheHolder == null) {
            optCacheElement = Optional.fromNullable(getCacheElement(cacheKey));
        } else {
            optCacheElement = cacheHolder.object;
        }

        return optCacheElement.isPresent() ? recalculate(cacheKey, optCacheElement.get()) : optCacheElement;
    }

    @Nullable
    @SuppressWarnings("unchecked")
    private <T> CacheElement<T> getCacheElement(@Nonnull CacheKey<T> cacheKey) {
        checkNotNull(cacheKey);
        return (CacheElement<T>) mCache.getIfPresent(cacheKey);
    }

    @Nonnull
    private <T> Optional<CacheElement<T>> recalculate(CacheKey<T> cacheKey, CacheElement<T> cacheElement) {
        checkNotNull(cacheKey);
        checkNotNull(cacheElement);
        final ImmutableList<CommandToExecute<T, ?>> commandsToExecutes = getCommandsToExecute(cacheElement);

        boolean hasChanged = false;

        T object = cacheElement.object;
        for (CommandToExecute<T, ?> commandToExecute : commandsToExecutes) {
            final Command.UpdateResult<T> result = executeCommand(object, commandToExecute);
            if (result.isInvalidate()) {

                final CacheHolder<T> cacheHolder = getCacheHolder(cacheKey);
                if (cacheHolder != null) {
                    cacheHolder.object = Optional.absent();
                }
                mCache.invalidate(cacheKey);

                if (cacheHolder != null) {
                    cacheHolder.onInvalidated();
                }
                return Optional.absent();
            } else if (result.isUpdate()) {
                object = result.getParam();
                hasChanged = true;
            }
        }
        cacheElement.version = mVersion++;

        if (hasChanged) {
            final CacheHolder<T> cacheHolder = getCacheHolder(cacheKey);
            if (cacheHolder != null) {
                cacheHolder.object.get().object = object;
                cacheHolder.onChanged(object);
            }
        }

        return Optional.of(cacheElement);
    }

    @Nonnull
    @SuppressWarnings("unchecked")
    private <T> ImmutableList<CommandToExecute<T, ?>> getCommandsToExecute(@Nonnull CacheElement<T> cacheElement) {
        checkNotNull(cacheElement);
        final ImmutableList<CommandToExecute<?, ?>> commandToExecutes = FluentIterable
                .from(mToExecute)
                .filter(CommandToExecute.matchingCacheElement(cacheElement))
                .toSortedList(CommandToExecute.SORT_VERSION_ASC);
        return (ImmutableList<CommandToExecute<T, ?>>)(ImmutableList<?>)commandToExecutes;
    }

    @Nonnull
    public <T> Optional<T> getCacheIfPresent(@Nonnull CacheKey<T> cacheKey) {
        checkNotNull(cacheKey);
        return getCacheElementIfPresent(cacheKey)
                .transform(CacheElement.<T>toObject());
    }

    int mVersion = 1;

    public <T> void putCache(@Nonnull CacheKey<T> cacheKey,
                             @Nonnull T object,
                             @Nonnull List<CommandName<T, ?>> commands) {
        checkNotNull(cacheKey);
        checkNotNull(object);
        checkNotNull(commands);

        final CacheElement<T> cacheElement = new CacheElement<>(mVersion++, object, commands);
        mCache.put(cacheKey, cacheElement);
        final CacheHolder<T> cacheHolder = getCacheHolder(cacheKey);
        if (cacheHolder != null) {
            cacheHolder.object = Optional.of(cacheElement);
            cacheHolder.onChanged(object);
        }
    }

    public <T> void unregister(@Nonnull CacheKey<T> cacheKey,
                               @Nonnull CacheInvalidationListener<T> listener) {
        checkNotNull(cacheKey);
        checkNotNull(listener);

        final CacheHolder<T> cacheHolder = getCacheHolder(cacheKey);
        checkState(cacheHolder != null, "Already unregistered all listeners");
        assert cacheHolder != null;
        checkState(cacheHolder.references.remove(listener), "Not registered this listener");
        if (cacheHolder.references.isEmpty()) {
            mHardReferenceStore.remove(cacheKey);
        }
    }

    public void clearAll() {
        mCache.invalidateAll();
        for (CacheHolder<?> cacheHolder : mHardReferenceStore.values()) {
            cacheHolder.object = Optional.absent();
            cacheHolder.onInvalidated();
        }
    }

}
