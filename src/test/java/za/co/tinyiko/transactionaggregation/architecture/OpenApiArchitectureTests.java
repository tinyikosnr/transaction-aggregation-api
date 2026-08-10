package za.co.tinyiko.transactionaggregation.architecture;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import za.co.tinyiko.transactionaggregation.TransactionAggregationApiApplication;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Confirms, by scanning every compiled class under the application's base package, that OpenAPI
 * annotations ({@code io.swagger.v3.oas.annotations.*}) appear only in the two places CLAUDE.md's
 * "Where New Things Belong" table designates as the presentation boundary: {@code api.controller}/
 * {@code api.dto} (and, for the one document-root bean, {@code config}). No domain, application,
 * port, or persistence class - in any module - should ever need to import a Swagger/OpenAPI type;
 * doing so would be exactly the kind of framework leak {@code FrameworkIndependenceAssertions}
 * already guards against for Spring/Jakarta Persistence types, one framework further.
 *
 * <p>A directory walk over {@code target/classes} (the same technique {@code ModularityTests}
 * relies on indirectly via Spring Modulith's own package scanning) is used instead of a hand-kept
 * type list like {@code ApiArchitectureTests}': the whole point of this check is to catch a
 * class no one thought to add to a list.
 */
class OpenApiArchitectureTests {

	private static final String BASE_PACKAGE = "za.co.tinyiko.transactionaggregation";
	private static final String SWAGGER_ANNOTATION_PACKAGE = "io.swagger.v3.oas.annotations";

	private static final List<String> ALLOWED_PACKAGE_PREFIXES = List.of(
			BASE_PACKAGE + ".api.controller",
			BASE_PACKAGE + ".api.dto",
			BASE_PACKAGE + ".config"
	);

	@Test
	void openApiAnnotationsOnlyAppearAtThePresentationBoundary() throws IOException, URISyntaxException {
		Path classesRoot = Path.of(TransactionAggregationApiApplication.class.getProtectionDomain()
				.getCodeSource().getLocation().toURI());

		List<String> violations = new ArrayList<>();

		try (Stream<Path> paths = Files.walk(classesRoot)) {
			paths.filter(path -> path.toString().endsWith(".class"))
					.map(path -> toClassName(classesRoot, path))
					.filter(name -> name.startsWith(BASE_PACKAGE))
					.filter(name -> !name.contains("package-info"))
					.filter(name -> ALLOWED_PACKAGE_PREFIXES.stream().noneMatch(name::startsWith))
					.forEach(name -> violations.addAll(swaggerAnnotationUsagesIn(loadClass(name))));
		}

		assertThat(violations)
				.as("OpenAPI annotations must only appear under %s", ALLOWED_PACKAGE_PREFIXES)
				.isEmpty();
	}

	private static String toClassName(Path root, Path classFile) {
		String relative = root.relativize(classFile).toString();
		String withoutExtension = relative.substring(0, relative.length() - ".class".length());
		return withoutExtension.replace('\\', '.').replace('/', '.');
	}

	private static Class<?> loadClass(String name) {
		try {
			return Class.forName(name, false, OpenApiArchitectureTests.class.getClassLoader());
		} catch (ClassNotFoundException | NoClassDefFoundError e) {
			throw new IllegalStateException("Could not load " + name, e);
		}
	}

	private static List<String> swaggerAnnotationUsagesIn(Class<?> type) {
		List<String> usages = new ArrayList<>();

		usages.addAll(swaggerNamesOf(type.getDeclaredAnnotations(), type.getName()));

		for (Field field : type.getDeclaredFields()) {
			usages.addAll(swaggerNamesOf(field.getDeclaredAnnotations(), type.getName() + "#" + field.getName()));
		}
		for (Method method : type.getDeclaredMethods()) {
			usages.addAll(swaggerNamesOf(method.getDeclaredAnnotations(), type.getName() + "#" + method.getName() + "()"));
			for (var parameter : method.getParameters()) {
				usages.addAll(swaggerNamesOf(parameter.getDeclaredAnnotations(),
						type.getName() + "#" + method.getName() + "(" + parameter.getName() + ")"));
			}
		}
		for (Constructor<?> constructor : type.getDeclaredConstructors()) {
			usages.addAll(swaggerNamesOf(constructor.getDeclaredAnnotations(), type.getName() + "#<init>"));
			for (var parameter : constructor.getParameters()) {
				usages.addAll(swaggerNamesOf(parameter.getDeclaredAnnotations(),
						type.getName() + "#<init>(" + parameter.getName() + ")"));
			}
		}

		return usages;
	}

	private static List<String> swaggerNamesOf(Annotation[] annotations, String location) {
		List<String> matches = new ArrayList<>();
		for (Annotation annotation : annotations) {
			String annotationName = annotation.annotationType().getName();
			if (annotationName.startsWith(SWAGGER_ANNOTATION_PACKAGE)) {
				matches.add(location + " carries " + annotationName);
			}
		}
		return matches;
	}

}
