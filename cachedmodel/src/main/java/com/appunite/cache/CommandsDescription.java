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
        public CommandName<T, K> commandName;
        public K parameter;

        public CommandToExecute(int version, CommandName<T, K> commandName, K parameter) {
            this.version = version;
            this.commandName = commandName;
            this.parameter = parameter;
        }

        public static <T> Predicate<? super CommandToExecute<?, ?>> matchingCommandsNames(final List<CommandName<T, ?>> commandsNames) {
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

        public static Predicate<? super CommandToExecute<?, ?>> matchingVersionGraterOrEqualTo(final int version) {
            return new Predicate<CommandToExecute<?, ?>>() {
                @Override
                public boolean apply(@Nullable CommandToExecute<?, ?> input) {
                    return input != null && input.version >= version;
                }
            };
        }

        public static <T> Predicate<? super CommandToExecute<?, ?>> matchingCacheElement(final CacheElement<T> cacheElement) {
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
        public T object;
        public List<CommandName<T, ?>> commands;

        public CacheElement(int version, T object, List<CommandName<T, ?>> commands) {
            this.version = version;
            this.object = object;
            this.commands = commands;
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
        Optional<CacheElement<T>> object;
        List<CacheInvalidationListener<T>> references = Lists.newArrayList();

        private CacheHolder(Optional<CacheElement<T>> object) {
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

    private HashMap<CommandName<?, ?>, Command<?,?>> mCommands = Maps.newHashMap();
    private List<CommandToExecute<?, ?>> mToExecute = Lists.newArrayList();
    private Cache<Object, CacheElement<?>> mCache = CacheBuilder.newBuilder()
            .maximumSize(20)
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .build();
    private Map<CacheKey<?>, CacheHolder<?>> mHardReferenceStore = Maps.newHashMap();

    public CommandsDescription() {
    }

    public <T, K> void addCommand(CommandName<T, K> commandName, Command<T, K> command) {
        mCommands.put(commandName, command);
    }

    public <T, K> void invalidate(CommandName<T, K> commandName, K parameter) {
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

    public <T> void register(CacheKey<T> cacheKey,
                                CacheInvalidationListener<T> listener) {
        checkNotNull(cacheKey);
        checkNotNull(listener);

        final CacheHolder<T> cacheHolder = getCacheHolderOrCreate(cacheKey);
        cacheHolder.references.add(listener);
    }

    @SuppressWarnings("unchecked")
    private <T, K> Command<T, K> getCommand(CommandToExecute<T, K> commandToExecute) {
        return (Command<T, K>) mCommands.get(commandToExecute.commandName);
    }

    @SuppressWarnings("unchecked")
    private <T> CacheHolder<T> getCacheHolder(CacheKey<T> cacheKey) {
        return (CacheHolder<T>) mHardReferenceStore.get(cacheKey);
    }

    private <T, K> Command.UpdateResult executeCommand(T object, CommandToExecute<T, K> commandToExecute) {
        final Command<T, K> command = getCommand(commandToExecute);
        return command.apply(commandToExecute.parameter, object);
    }

    private <T> CacheHolder<T> getCacheHolderOrCreate(CacheKey<T> cacheKey) {
        CacheHolder<T> cacheHolder = getCacheHolder(cacheKey);
        if (cacheHolder == null) {
            final Optional<CacheElement<T>> cacheIfPresent = getCacheElementIfPresent(cacheKey);
            cacheHolder = new CacheHolder<>(cacheIfPresent);
            mHardReferenceStore.put(cacheKey, cacheHolder);
        }
        return cacheHolder;
    }

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

    @SuppressWarnings("unchecked")
    private <T> CacheElement<T> getCacheElement(CacheKey<T> cacheKey) {
        return (CacheElement<T>) mCache.getIfPresent(cacheKey);
    }

    private <T> Optional<CacheElement<T>> recalculate(CacheKey<T> cacheKey, CacheElement<T> cacheElement) {

        final ImmutableList<CommandToExecute<T, ?>> commandsToExecutes = getCommandsToExecute(cacheElement);

        boolean hasChanged = false;

        final T object = cacheElement.object;
        for (CommandToExecute<T, ?> commandToExecute : commandsToExecutes) {
            final Command.UpdateResult result = executeCommand(object, commandToExecute);
            if (Command.UpdateResult.INVALIDATE.equals(result)) {

                final CacheHolder<T> cacheHolder = getCacheHolder(cacheKey);
                if (cacheHolder != null) {
                    cacheHolder.object = Optional.absent();
                }
                mCache.invalidate(cacheKey);

                if (cacheHolder != null) {
                    cacheHolder.onInvalidated();
                }
                return Optional.absent();
            } else if (Command.UpdateResult.UPDATED.equals(result)) {
                hasChanged = true;
            }
        }
        cacheElement.version = mVersion++;

        if (hasChanged) {
            final CacheHolder<T> cacheHolder = getCacheHolder(cacheKey);
            if (cacheHolder != null) {
                cacheHolder.onChanged(object);
            }
        }

        return Optional.of(cacheElement);
    }

    @SuppressWarnings("unchecked")
    private <T> ImmutableList<CommandToExecute<T, ?>> getCommandsToExecute(CacheElement<T> cacheElement) {
        final ImmutableList<CommandToExecute<?, ?>> commandToExecutes = FluentIterable
                .from(mToExecute)
                .filter(CommandToExecute.matchingCacheElement(cacheElement))
                .toSortedList(CommandToExecute.SORT_VERSION_ASC);
        return (ImmutableList<CommandToExecute<T, ?>>)(ImmutableList<?>)commandToExecutes;
    }

    public <T> Optional<T> getCacheIfPresent(CacheKey<T> cacheKey) {
        checkNotNull(cacheKey);
        return getCacheElementIfPresent(cacheKey)
                .transform(CacheElement.<T>toObject());
    }

    int mVersion = 1;

    public <T> void putCache(CacheKey<T> cacheKey, T object,
                             List<CommandName<T, ?>> commands) {
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

    public <T> void unregister(CacheKey<T> cacheKey, CacheInvalidationListener<T> listener) {
        checkNotNull(cacheKey);

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
