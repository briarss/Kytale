package aster.amo.hykot.util

import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/**
 * Thread-safe cache with automatic expiration.
 *
 * Example:
 * ```kotlin
 * val playerDataCache = ExpiringCache<UUID, PlayerData>(5.minutes)
 *
 * fun getPlayerData(id: UUID): PlayerData {
 *     return playerDataCache.getOrPut(id) {
 *         loadFromDatabase(id)
 *     }
 * }
 * ```
 *
 * @param K the key type
 * @param V the value type
 * @property ttl time-to-live for cache entries
 * @property timeSource the time source for expiration
 */
class ExpiringCache<K : Any, V : Any>(
    private val ttl: Duration,
    private val timeSource: TimeSource = TimeSource.Monotonic
) {
    @PublishedApi
    internal data class Entry<V>(val value: V, val created: TimeMark)

    @PublishedApi
    internal val cache = ConcurrentHashMap<K, Entry<V>>()

    /**
     * Gets a value from the cache, or null if not present or expired.
     *
     * @param key the key
     * @return the cached value or null
     */
    fun get(key: K): V? {
        val entry = cache[key] ?: return null
        return if (entry.created.elapsedNow() < ttl) {
            entry.value
        } else {
            cache.remove(key)
            null
        }
    }

    /**
     * Gets a value from the cache, or computes and stores a new value.
     *
     * @param key the key
     * @param compute function to compute the value if missing
     * @return the cached or computed value
     */
    inline fun getOrPut(key: K, compute: () -> V): V {
        val existing = get(key)
        if (existing != null) return existing

        val newValue = compute()
        put(key, newValue)
        return newValue
    }

    /**
     * Stores a value in the cache.
     *
     * @param key the key
     * @param value the value to store
     */
    fun put(key: K, value: V) {
        cache[key] = Entry(value, timeSource.markNow())
    }

    /**
     * Removes a value from the cache.
     *
     * @param key the key to remove
     * @return the removed value, or null
     */
    fun remove(key: K): V? = cache.remove(key)?.value

    /**
     * Checks if the cache contains a non-expired entry.
     *
     * @param key the key to check
     * @return true if present and not expired
     */
    fun contains(key: K): Boolean = get(key) != null

    /**
     * Invalidates (removes) a cache entry.
     *
     * @param key the key to invalidate
     */
    fun invalidate(key: K) {
        cache.remove(key)
    }

    /**
     * Invalidates all entries matching the predicate.
     *
     * @param predicate the condition for removal
     */
    inline fun invalidateIf(crossinline predicate: (K) -> Boolean) {
        cache.keys.removeIf { predicate(it) }
    }

    /**
     * Clears all cache entries.
     */
    fun clear() {
        cache.clear()
    }

    /**
     * Removes expired entries from the cache.
     *
     * Call periodically to prevent memory buildup.
     */
    fun cleanup() {
        cache.entries.removeIf { (_, entry) ->
            entry.created.elapsedNow() >= ttl
        }
    }

    /**
     * Gets all non-expired entries.
     *
     * @return a map of valid entries
     */
    fun entries(): Map<K, V> {
        cleanup()
        return cache.mapValues { it.value.value }
    }

    /**
     * Gets the number of entries (including potentially expired ones).
     */
    val size: Int get() = cache.size
}

/**
 * Simple non-expiring concurrent cache.
 *
 * @param K the key type
 * @param V the value type
 */
class SimpleCache<K : Any, V : Any> {
    @PublishedApi
    internal val cache = ConcurrentHashMap<K, V>()

    fun get(key: K): V? = cache[key]

    inline fun getOrPut(key: K, compute: () -> V): V = cache.getOrPut(key, compute)

    fun put(key: K, value: V): V? = cache.put(key, value)

    fun remove(key: K): V? = cache.remove(key)

    fun contains(key: K): Boolean = cache.containsKey(key)

    fun clear() = cache.clear()

    fun entries(): Map<K, V> = cache.toMap()

    fun keys(): Set<K> = cache.keys.toSet()

    fun values(): Collection<V> = cache.values.toList()

    val size: Int get() = cache.size

    val isEmpty: Boolean get() = cache.isEmpty()

    val isNotEmpty: Boolean get() = cache.isNotEmpty()
}

/**
 * LRU (Least Recently Used) cache with size limit.
 *
 * @param K the key type
 * @param V the value type
 * @property maxSize maximum number of entries
 */
class LRUCache<K : Any, V : Any>(
    private val maxSize: Int
) {
    @PublishedApi
    internal val cache = object : LinkedHashMap<K, V>(maxSize, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>?): Boolean {
            return size > maxSize
        }
    }

    @PublishedApi
    internal val lock = Any()

    @Synchronized
    fun get(key: K): V? = cache[key]

    inline fun getOrPut(key: K, compute: () -> V): V = synchronized(lock) {
        cache[key] ?: compute().also { cache[key] = it }
    }

    @Synchronized
    fun put(key: K, value: V): V? = cache.put(key, value)

    @Synchronized
    fun remove(key: K): V? = cache.remove(key)

    @Synchronized
    fun contains(key: K): Boolean = cache.containsKey(key)

    @Synchronized
    fun clear() = cache.clear()

    @Synchronized
    fun entries(): Map<K, V> = cache.toMap()

    val size: Int @Synchronized get() = cache.size
}

/**
 * Computed cache that lazily loads values on demand.
 *
 * @param K the key type
 * @param V the value type
 * @property loader function to load values
 */
class LoadingCache<K : Any, V : Any>(
    private val loader: (K) -> V
) {
    private val cache = ConcurrentHashMap<K, V>()

    /**
     * Gets a value, loading it if not present.
     *
     * @param key the key
     * @return the cached or loaded value
     */
    fun get(key: K): V = cache.getOrPut(key) { loader(key) }

    /**
     * Gets a value without loading, or null if not cached.
     *
     * @param key the key
     * @return the cached value or null
     */
    fun getIfPresent(key: K): V? = cache[key]

    /**
     * Forces a value to be reloaded.
     *
     * @param key the key to refresh
     * @return the newly loaded value
     */
    fun refresh(key: K): V {
        val value = loader(key)
        cache[key] = value
        return value
    }

    /**
     * Stores a value directly.
     *
     * @param key the key
     * @param value the value
     */
    fun put(key: K, value: V) {
        cache[key] = value
    }

    /**
     * Invalidates a cache entry.
     *
     * @param key the key to invalidate
     */
    fun invalidate(key: K) {
        cache.remove(key)
    }

    /**
     * Clears all entries.
     */
    fun clear() = cache.clear()

    val size: Int get() = cache.size
}

/**
 * Creates an expiring cache.
 *
 * @param ttl time-to-live for entries
 */
fun <K : Any, V : Any> expiringCache(ttl: Duration): ExpiringCache<K, V> = ExpiringCache(ttl)

/**
 * Creates a simple cache.
 */
fun <K : Any, V : Any> simpleCache(): SimpleCache<K, V> = SimpleCache()

/**
 * Creates an LRU cache.
 *
 * @param maxSize maximum size
 */
fun <K : Any, V : Any> lruCache(maxSize: Int): LRUCache<K, V> = LRUCache(maxSize)

/**
 * Creates a loading cache.
 *
 * @param loader the value loader function
 */
fun <K : Any, V : Any> loadingCache(loader: (K) -> V): LoadingCache<K, V> = LoadingCache(loader)
