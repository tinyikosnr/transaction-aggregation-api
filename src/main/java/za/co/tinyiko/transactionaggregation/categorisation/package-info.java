/**
 * Categorisation module — owns {@code transaction_categories} and {@code categorisation_rules}.
 *
 * <p>Responsible for category and rule management, and for evaluating configurable
 * merchant/description matching rules to assign a transaction's category. Despite TDS 5
 * listing "Merchant module" under this module's dependencies, that is a business-level
 * statement, not a code dependency: categorisation receives merchant text as a plain string
 * (see {@code CategorisationInput}), never calling merchant's ports or referencing its types.
 * Categorisation depends on nothing but {@code shared} - enforced below, since with no other
 * module depending on the default permissive dependency graph, this is now a genuine,
 * verifiable rule rather than documentation alone.
 */
@org.springframework.modulith.ApplicationModule(allowedDependencies = {})
package za.co.tinyiko.transactionaggregation.categorisation;
