Map<String, Object> clientCredentials = new HashMap<>();
    clientCredentials.put("secret", secret);
    clientCredentials.put("provider", "secret");

    Configuration configuration =
            new Configuration(keycloakUrl, realm, clientId, clientCredentials, null);

    AuthzClient authzClient = AuthzClient.create(configuration);