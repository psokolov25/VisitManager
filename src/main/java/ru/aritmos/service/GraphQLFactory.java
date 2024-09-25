package ru.aritmos.service;

import graphql.GraphQL;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.core.io.ResourceResolver;
import jakarta.inject.Singleton;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Optional;

@Factory // (1)
public class GraphQLFactory {

  @Bean
  @Singleton
  public GraphQL graphQL(
      ResourceResolver resourceResolver, BranchFetchData helloDataFetcher) { // (2)

    SchemaParser schemaParser = new SchemaParser();
    SchemaGenerator schemaGenerator = new SchemaGenerator();
    Optional<InputStream> inputStream =
        resourceResolver.getResourceAsStream("classpath:schema.graphqls");
    // Parse the schema.
    TypeDefinitionRegistry typeRegistry = new TypeDefinitionRegistry();
    if (inputStream.isPresent()) {
      typeRegistry.merge(
          schemaParser.parse(new BufferedReader(new InputStreamReader(inputStream.get()))));

      // Create the runtime wiring.
      RuntimeWiring runtimeWiring =
          RuntimeWiring.newRuntimeWiring()
              .type("Query", typeWiring -> typeWiring.dataFetcher("getBranch", helloDataFetcher))
              .build();

      // Create the executable schema.
      GraphQLSchema graphQLSchema =
          schemaGenerator.makeExecutableSchema(typeRegistry, runtimeWiring);

      // Return the GraphQL bean.
      return GraphQL.newGraphQL(graphQLSchema).build();
    }
    return null;
  }
}
