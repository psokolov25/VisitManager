package ru.aritmos.test;

import io.micronaut.cache.CacheManager;
import io.micronaut.cache.SyncCache;
import io.micronaut.context.annotation.Replaces;
import io.micronaut.core.type.Argument;
import jakarta.inject.Singleton;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Тестовый менеджер кэша, отключающий реальное кэширование.
 */
@Singleton
@Replaces(io.micronaut.cache.DefaultCacheManager.class)
public class NoopCacheManager implements CacheManager<Object> {

  @Override
  public SyncCache<Object> getCache(String name) {
    return new NoopSyncCache(name);
  }

  @Override
  public java.util.Set<String> getCacheNames() { return java.util.Set.of(); }

  static final class NoopSyncCache implements SyncCache<Object> {
    private final String name;

    NoopSyncCache(String name) {
      this.name = name;
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public Object getNativeCache() {
      return null;
    }

    @Override
    public <T> Optional<T> get(Object key, Argument<T> requiredType) {
      return Optional.empty();
    }

    @Override
    public <T> Optional<T> get(Object key, Class<T> requiredType) {
      return Optional.empty();
    }

    @Override
    public <T> T get(Object key, Argument<T> requiredType, Supplier<T> supplier) {
      return supplier.get();
    }

    // default method exists for (Class<T>, Supplier<T>)

    @Override
    public void put(Object key, Object value) {
      // no-op
    }

    @Override
    public <T> java.util.Optional<T> putIfAbsent(Object key, T value) {
      return java.util.Optional.ofNullable(value);
    }

    @Override
    public void invalidate(Object key) {
      // no-op
    }

    @Override
    public void invalidateAll() {
      // no-op
    }
  }
}
