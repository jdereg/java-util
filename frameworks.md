# 🚀 Framework Integration Examples

java-util integrates seamlessly with popular Java frameworks and platforms:

## Spring Framework Integration

**Configuration and Caching:**
```java
@Configuration
public class CacheConfig {
    
    @Bean
    @Primary
    public CacheManager javaUtilCacheManager() {
        return new CacheManager() {
            private final TTLCache<String, Object> cache = 
                new TTLCache<>(30 * 60 * 1000, 10000); // 30 minutes in milliseconds
            
            @Override
            public Cache getCache(String name) {
                return new SimpleValueWrapper(cache);
            }
        };
    }
    
    @Bean
    public CaseInsensitiveMap<String, String> applicationProperties() {
        return new CaseInsensitiveMap<>(new ConcurrentHashMap<>());
    }
}

@Service  
public class DataService {
    @Autowired
    private CaseInsensitiveMap<String, String> properties;
    
    public String getConfig(String key) {
        // Case-insensitive property lookup
        return properties.get(key); // Works with "API_KEY", "api_key", "Api_Key"
    }
}
```

## Jakarta EE / JEE Integration

**CDI Producers and Validation:**
```java
@ApplicationScoped
public class UtilityProducers {
    
    @Produces
    @ApplicationScoped
    public Converter typeConverter() {
        return new Converter(); // Thread-safe singleton
    }
    
    @Produces 
    @RequestScoped
    public LRUCache<String, UserSession> sessionCache() {
        return new LRUCache<>(1000, LRUCache.StrategyType.THREADED);
    }
}

@Stateless
public class ValidationService {
    @Inject
    private Converter converter;
    
    public <T> T validateAndConvert(Object input, Class<T> targetType) {
        if (input == null) return null;
        
        try {
            return converter.convert(input, targetType);
        } catch (Exception e) {
            throw new ValidationException("Cannot convert to " + targetType.getName());
        }
    }
}
```

## Spring Boot Auto-Configuration

**Custom Auto-Configuration:**
```java
@Configuration
@ConditionalOnClass(Converter.class)
@EnableConfigurationProperties(JavaUtilProperties.class)
public class JavaUtilAutoConfiguration {
    
    @Bean
    @ConditionalOnMissingBean
    public Converter defaultConverter(JavaUtilProperties properties) {
        Converter converter = new Converter();
        
        // Apply security settings from application.yml
        if (properties.getSecurity().isEnabled()) {
            System.setProperty("converter.security.enabled", "true");
        }
        
        return converter;
    }
    
    @Bean
    @ConditionalOnProperty(prefix = "java-util.cache", name = "enabled", havingValue = "true")
    public TTLCache<String, Object> applicationCache(JavaUtilProperties properties) {
        return new TTLCache<>(
            properties.getCache().getTtlMinutes() * 60 * 1000, // Convert minutes to milliseconds
            properties.getCache().getMaxSize()
        );
    }
}

@ConfigurationProperties(prefix = "java-util")
@Data
public class JavaUtilProperties {
    private Security security = new Security();
    private Cache cache = new Cache();
    
    @Data
    public static class Security {
        private boolean enabled = false;
        private int maxCollectionSize = 1000000;
    }
    
    @Data  
    public static class Cache {
        private boolean enabled = true;
        private int ttlMinutes = 30;
        private int maxSize = 10000;
    }
}
```

## Microservices & Cloud Native

**Service Discovery & Configuration:**
```java
@Component
public class ConfigurationManager {
    private final CaseInsensitiveMap<String, String> envConfig;
    private final TTLCache<String, ServiceInstance> serviceCache;
    
    public ConfigurationManager() {
        // Environment variables (case-insensitive)
        this.envConfig = new CaseInsensitiveMap<>();
        System.getenv().forEach(envConfig::put);
        
        // Service discovery cache (5 minute TTL in milliseconds)
        this.serviceCache = new TTLCache<>(5 * 60 * 1000, 1000);
    }
    
    public String getConfigValue(String key) {
        // Works with SPRING_PROFILES_ACTIVE, spring_profiles_active, etc.
        return envConfig.get(key);
    }
    
    @EventListener
    public void onServiceDiscovery(ServiceRegisteredEvent event) {
        serviceCache.put(event.getServiceId(), event.getServiceInstance());
    }
}
```

## Testing Integration

**Enhanced Test Comparisons:**
```java
@TestConfiguration
public class TestConfig {
    
    @Bean
    @Primary
    public TestDataComparator testComparator() {
        return new TestDataComparator();
    }
}

public class TestDataComparator {
    private final Map<String, Object> options = new HashMap<>();
    
    public void assertDeepEquals(Object expected, Object actual, String message) {
        options.clear();
        boolean equals = DeepEquals.deepEquals(expected, actual, options);
        
        if (!equals) {
            String diff = (String) options.get("diff");
            fail(message + "\nDifferences:\n" + diff);
        }
    }
    
    public <T> T roundTripConvert(Object source, Class<T> targetType) {
        Converter converter = new Converter();
        return converter.convert(source, targetType);
    }
}

@ExtendWith(SpringExtension.class)
class IntegrationTest {
    @Autowired
    private TestDataComparator comparator;
    
    @Test
    void testComplexDataProcessing() {
        ComplexData expected = createExpectedData();
        ComplexData actual = processData();
        
        // Handles cycles, nested collections, etc.
        comparator.assertDeepEquals(expected, actual, "Data processing failed");
    }
}
```

## Performance Monitoring Integration

**Micrometer Metrics:**
```java
@Component
public class CacheMetrics {
    private final MeterRegistry meterRegistry;
    private final TTLCache<String, Object> cache;
    
    @EventListener
    @Async
    public void onCacheAccess(CacheAccessEvent event) {
        Timer.Sample sample = Timer.start(meterRegistry);
        
        if (event.isHit()) {
            meterRegistry.counter("cache.hits", "cache", event.getCacheName()).increment();
        } else {
            meterRegistry.counter("cache.misses", "cache", event.getCacheName()).increment();  
        }
        
        sample.stop(Timer.builder("cache.access.duration")
            .tag("cache", event.getCacheName())
            .register(meterRegistry));
    }
}
```

## Constructor Reference

For reference, here are the correct constructor signatures:

### TTLCache Constructors
```java
// All TTL parameters are in milliseconds
public TTLCache(long ttlMillis)
public TTLCache(long ttlMillis, int maxSize)  
public TTLCache(long ttlMillis, int maxSize, long cleanupIntervalMillis)
```

### LRUCache Constructors
```java
// Capacity is number of entries
public LRUCache(int capacity)  // Uses LOCKING strategy
public LRUCache(int capacity, StrategyType strategyType)
public LRUCache(int capacity, int cleanupDelayMillis)  // Uses THREADED strategy
```