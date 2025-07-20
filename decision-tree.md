# MultiKeyMap as O(1) Decision Table

MultiKeyMap excels as an **O(1) decision table** when using Lists or Maps as key components, enabling instant equality-based lookups across unlimited dimensions with unlimited output parameters.

## Equality-Based Matching (Core Strength)

**Important:** MultiKeyMap performs **equality matching** on keys (using `.equals()` and `.hashCode()`), not relational operations like `>`, `<`, `>=`, or `<=`. This equality-based approach is what enables O(1) hash table performance.

**Best Practice - Hybrid Approach:**
For decisions requiring both equality matching and relational operations, use MultiKeyMap for the equality-based dimensions and combine with traditional logic for relational criteria:

```java
// Use MultiKeyMap for equality-based criteria (O(1))
MultiKeyMap<Map<String, Object>> equalityRules = new MultiKeyMap<>();
equalityRules.put(baseDecision, "enterprise", "north-america", "credit-card");

// Combine with relational logic for numeric/range criteria
Map<String, Object> decision = equalityRules.get(customerType, region, paymentMethod);
if (decision != null && orderAmount > 10000) {
    decision.put("volumeDiscount", 5.0);  // Add volume-based discount
}
if (decision != null && customerAge < 25) {
    decision.put("youthDiscount", 2.0);   // Add age-based discount  
}
```

This hybrid approach leverages MultiKeyMap's O(1) performance for categorical/enum-like criteria while handling numeric ranges through efficient conditional logic.

## Basic Decision Table Pattern

**Business Rules Engine:**
```java
// Create decision table with rich structured results
MultiKeyMap<Map<String, Object>> businessRules = new MultiKeyMap<>();

// Define decision dimensions (input criteria)
String customerTier = "enterprise";
String region = "north-america";
String orderVolume = "high";
String paymentMethod = "credit";

// Define rich decision result (multiple outputs)
Map<String, Object> pricingDecision = Map.of(
    "baseDiscount", 15.0,
    "expeditedShipping", true,
    "accountManager", "senior-team",
    "approvalRequired", false,
    "creditTerms", "net-30",
    "volumeBonus", 500.00
);

// Store the decision rule - O(1) insertion
businessRules.put(pricingDecision, customerTier, region, orderVolume, paymentMethod);

// Execute business rule - O(1) lookup, no rule iteration needed!
Map<String, Object> decision = businessRules.get("enterprise", "north-america", "high", "credit");

// Extract multiple decision outputs
double discount = (Double) decision.get("baseDiscount");        // 15.0
boolean expedited = (Boolean) decision.get("expeditedShipping"); // true
String manager = (String) decision.get("accountManager");       // "senior-team"
```

## Decision Table Visualization

The MultiKeyMap acts as a **4-dimensional decision table** where each combination of input criteria maps to a rich set of business outputs:

| Customer Tier | Region | Order Volume | Payment Method | → Decision Result |
|---------------|--------|--------------|----------------|-------------------|
| `"enterprise"` | `"north-america"` | `"high"` | `"credit"` | `{baseDiscount: 15.0, expeditedShipping: true, accountManager: "senior-team", approvalRequired: false, creditTerms: "net-30", volumeBonus: 500.00}` |
| `"premium"` | `"europe"` | `"medium"` | `"wire"` | `{baseDiscount: 12.0, expeditedShipping: false, accountManager: "standard-team", approvalRequired: true, creditTerms: "net-15", volumeBonus: 200.00}` |
| `"standard"` | `"asia-pacific"` | `"low"` | `"credit"` | `{baseDiscount: 5.0, expeditedShipping: false, accountManager: "self-service", approvalRequired: true, creditTerms: "prepaid", volumeBonus: 0.00}` |

## Advanced Decision Table with Business Objects

```java
// Decision table with complex business objects as results
MultiKeyMap<Map<String, BusinessObject>> advancedRules = new MultiKeyMap<>();

// Rich business decision with multiple typed objects
Map<String, BusinessObject> dealStructure = Map.of(
    "pricingTier", new PricingTier("enterprise", 15.0, "volume-discount"),
    "servicePlan", new ServicePlan("premium", true, "24x7-support"),
    "accountTeam", new AccountTeam("senior", "john.smith@company.com", "direct-line"),
    "contractTerms", new ContractTerms("annual", "net-30", "auto-renew"),
    "compliance", new ComplianceProfile("sox-compliant", "gdpr-ready", "audit-trail")
);

// Store complex business rule
advancedRules.put(dealStructure, "fortune-500", "financial-services", "multi-year", "enterprise-security");

// Execute complex business decision - still O(1)!
Map<String, BusinessObject> businessDecision = advancedRules.get(
    "fortune-500", "financial-services", "multi-year", "enterprise-security"
);

// Extract strongly-typed business objects
PricingTier pricing = (PricingTier) businessDecision.get("pricingTier");
ServicePlan service = (ServicePlan) businessDecision.get("servicePlan");
AccountTeam team = (AccountTeam) businessDecision.get("accountTeam");
ContractTerms terms = (ContractTerms) businessDecision.get("contractTerms");
ComplianceProfile compliance = (ComplianceProfile) businessDecision.get("compliance");
```

## Type-Safe Decision Table Façade

```java
// Wrap MultiKeyMap in type-safe business interface
public class EnterpriseRulesEngine {
    private final MultiKeyMap<Map<String, BusinessObject>> rules = new MultiKeyMap<>();
    
    public void defineRule(String customerType, String industry, String duration, 
                          String security, Map<String, BusinessObject> decision) {
        rules.put(decision, customerType, industry, duration, security);
    }
    
    public EnterpriseDecision evaluateRule(String customerType, String industry, 
                                         String duration, String security) {
        Map<String, BusinessObject> result = rules.get(customerType, industry, duration, security);
        return result != null ? new EnterpriseDecision(result) : null;
    }
    
    // Type-safe wrapper for decision results
    public static class EnterpriseDecision {
        private final Map<String, BusinessObject> decision;
        
        public EnterpriseDecision(Map<String, BusinessObject> decision) {
            this.decision = decision;
        }
        
        public PricingTier getPricing() { return (PricingTier) decision.get("pricingTier"); }
        public ServicePlan getService() { return (ServicePlan) decision.get("servicePlan"); }
        public AccountTeam getTeam() { return (AccountTeam) decision.get("accountTeam"); }
        public ContractTerms getTerms() { return (ContractTerms) decision.get("contractTerms"); }
        public ComplianceProfile getCompliance() { return (ComplianceProfile) decision.get("compliance"); }
    }
}
```

## Configuration Decision Tables

```java
MultiKeyMap<Properties> configDecisions = new MultiKeyMap<>();

// Environment + Feature + User Role = Configuration Set
List<String> environment = Arrays.asList("prod", "staging");
List<String> features = Arrays.asList("feature-A", "feature-B");
Map<String, String> userAttributes = Map.of("role", "admin", "region", "US");

Properties config = new Properties();
config.setProperty("cache.size", "1000");
config.setProperty("rate.limit", "100");
config.setProperty("debug.enabled", "false");

configDecisions.put(config, environment, features, userAttributes);

// Instant O(1) configuration resolution
Properties resolved = configDecisions.get(
    Arrays.asList("prod", "staging"),
    Arrays.asList("feature-A", "feature-B"),
    Map.of("role", "admin", "region", "US")
);
```

## Decision Table Performance Advantages

- **O(1) Rule Execution:** Direct hash lookup vs. sequential rule evaluation
- **Unlimited Input Dimensions:** Scale to any number of decision criteria  
- **Rich Output Results:** Maps/Objects enable unlimited structured decision outputs
- **Thread-Safe Decisions:** Concurrent rule evaluation for high-throughput systems
- **Type-Safe Integration:** Façade pattern provides compile-time safety over raw MultiKeyMap

## vs. Traditional Rule Engines

| Approach | Performance | Flexibility | Memory | Complexity |
|----------|-------------|-------------|--------|------------|
| **MultiKeyMap Decision Table** | **O(1)** | **Unlimited dimensions** | **Low** | **Simple** |
| Traditional Rule Engine | **O(n)** | Complex pattern matching | High | Complex |
| IF/ELSE chains | **O(n)** | Limited scalability | Low | Unmaintainable |
| Decision Trees | **O(log n)** | Binary decisions | Medium | Moderate |

This pattern transforms MultiKeyMap into a **high-performance business rules engine** where complex multi-dimensional decisions execute in constant time, regardless of the number of rules stored.

## Use Cases

- **Pricing Engines:** Multi-factor pricing with complex output structures
- **Configuration Management:** Environment-specific settings with rich metadata
- **Access Control:** Multi-dimensional permissions with detailed policy results
- **Content Routing:** Multi-criteria routing with routing metadata
- **Business Workflow:** State-based decisions with complex next-step information
- **Feature Flags:** Multi-dimensional feature control with rollout metadata