package com.function.graphql;

import com.function.repository.UserRepository;
import com.function.repository.RoleRepository;
import com.function.model.User;
import com.function.model.Role;
import graphql.GraphQL;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import graphql.schema.DataFetcher;

import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

public class GraphQLProvider {
    private static final Logger logger = Logger.getLogger(GraphQLProvider.class.getName());
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private GraphQL graphQL;

    public GraphQLProvider() {
        this.userRepository = new UserRepository();
        this.roleRepository = new RoleRepository();
        init();
    }

    private void init() {
        try {
            String sdl = buildSDL();
            TypeDefinitionRegistry typeRegistry = new SchemaParser().parse(sdl);
            RuntimeWiring runtimeWiring = buildWiring();
            GraphQLSchema schema = new SchemaGenerator().makeExecutableSchema(typeRegistry, runtimeWiring);
            this.graphQL = GraphQL.newGraphQL(schema).build();
        } catch (Exception e) {
            logger.severe("Error initializing GraphQL: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private String buildSDL() {
        return """
                type User {
                    id: ID!
                    username: String!
                    email: String!
                    fullName: String!
                    active: Boolean!
                    roles: [Role]
                }
                
                type Role {
                    id: ID!
                    name: String!
                    description: String
                }
                
                type Query {
                    users: [User]
                    user(id: ID!): User
                    roles: [Role]
                    role(id: ID!): Role
                    userRoles(userId: ID!): [Role]
                }
                
                type Mutation {
                    createUser(username: String!, email: String!, fullName: String!, password: String!, active: Boolean!): User
                    updateUser(id: ID!, username: String!, email: String!, fullName: String!, password: String!, active: Boolean!): User
                    deleteUser(id: ID!): Boolean
                    createRole(name: String!, description: String): Role
                    updateRole(id: ID!, name: String!, description: String): Role
                    deleteRole(id: ID!): Boolean
                    assignRoleToUser(userId: ID!, roleId: ID!): Boolean
                    removeRoleFromUser(userId: ID!, roleId: ID!): Boolean
                }
                """;
    }

    private RuntimeWiring buildWiring() {
        return RuntimeWiring.newRuntimeWiring()
                .type("Query", typeWiring -> typeWiring
                        .dataFetcher("users", usersDataFetcher())
                        .dataFetcher("user", userDataFetcher())
                        .dataFetcher("roles", rolesDataFetcher())
                        .dataFetcher("role", roleDataFetcher())
                        .dataFetcher("userRoles", userRolesDataFetcher()))
                .type("Mutation", typeWiring -> typeWiring
                        .dataFetcher("createUser", createUserDataFetcher())
                        .dataFetcher("updateUser", updateUserDataFetcher())
                        .dataFetcher("deleteUser", deleteUserDataFetcher())
                        .dataFetcher("createRole", createRoleDataFetcher())
                        .dataFetcher("updateRole", updateRoleDataFetcher())
                        .dataFetcher("deleteRole", deleteRoleDataFetcher())
                        .dataFetcher("assignRoleToUser", assignRoleToUserDataFetcher())
                        .dataFetcher("removeRoleFromUser", removeRoleFromUserDataFetcher()))
                .build();
    }

    // Query DataFetchers
    private DataFetcher<List<User>> usersDataFetcher() {
        return environment -> {
            try {
                return userRepository.findAll();
            } catch (Exception e) {
                logger.severe("Error fetching users: " + e.getMessage());
                throw new RuntimeException("Error fetching users", e);
            }
        };
    }

    private DataFetcher<User> userDataFetcher() {
        return environment -> {
            try {
                String id = environment.getArgument("id");
                Optional<User> user = userRepository.findById(Long.parseLong(id));
                return user.orElse(null);
            } catch (Exception e) {
                logger.severe("Error fetching user: " + e.getMessage());
                throw new RuntimeException("Error fetching user", e);
            }
        };
    }

    private DataFetcher<List<Role>> rolesDataFetcher() {
        return environment -> {
            try {
                return roleRepository.findAll();
            } catch (Exception e) {
                logger.severe("Error fetching roles: " + e.getMessage());
                throw new RuntimeException("Error fetching roles", e);
            }
        };
    }

    private DataFetcher<Role> roleDataFetcher() {
        return environment -> {
            try {
                String id = environment.getArgument("id");
                Optional<Role> role = roleRepository.findById(Long.parseLong(id));
                return role.orElse(null);
            } catch (Exception e) {
                logger.severe("Error fetching role: " + e.getMessage());
                throw new RuntimeException("Error fetching role", e);
            }
        };
    }

    private DataFetcher<List<Role>> userRolesDataFetcher() {
        return environment -> {
            try {
                String userId = environment.getArgument("userId");
                return roleRepository.findRolesByUserId(Long.parseLong(userId));
            } catch (Exception e) {
                logger.severe("Error fetching user roles: " + e.getMessage());
                throw new RuntimeException("Error fetching user roles", e);
            }
        };
    }

    // Mutation DataFetchers
    private DataFetcher<User> createUserDataFetcher() {
        return environment -> {
            try {
                String username = environment.getArgument("username");
                String email = environment.getArgument("email");
                String fullName = environment.getArgument("fullName");
                String password = environment.getArgument("password");
                boolean active = environment.getArgument("active");

                User user = new User();
                user.setUsername(username);
                user.setEmail(email);
                user.setFullName(fullName);
                user.setPassword(password); // In production, hash the password
                user.setActive(active);
                
                return userRepository.save(user);
            } catch (Exception e) {
                logger.severe("Error creating user: " + e.getMessage());
                throw new RuntimeException("Error creating user", e);
            }
        };
    }

    private DataFetcher<User> updateUserDataFetcher() {
        return environment -> {
            try {
                String id = environment.getArgument("id");
                String username = environment.getArgument("username");
                String email = environment.getArgument("email");
                String fullName = environment.getArgument("fullName");
                String password = environment.getArgument("password");
                boolean active = environment.getArgument("active");

                User user = new User();
                user.setId(Long.parseLong(id));
                user.setUsername(username);
                user.setEmail(email);
                user.setFullName(fullName);
                user.setPassword(password);
                user.setActive(active);
                
                return userRepository.save(user);
            } catch (Exception e) {
                logger.severe("Error updating user: " + e.getMessage());
                throw new RuntimeException("Error updating user", e);
            }
        };
    }

    private DataFetcher<Boolean> deleteUserDataFetcher() {
        return environment -> {
            try {
                String id = environment.getArgument("id");
                return userRepository.deleteById(Long.parseLong(id));
            } catch (Exception e) {
                logger.severe("Error deleting user: " + e.getMessage());
                throw new RuntimeException("Error deleting user", e);
            }
        };
    }

    private DataFetcher<Role> createRoleDataFetcher() {
        return environment -> {
            try {
                String name = environment.getArgument("name");
                String description = environment.getArgument("description");

                Role role = new Role();
                role.setName(name);
                role.setDescription(description);
                
                return roleRepository.save(role);
            } catch (Exception e) {
                logger.severe("Error creating role: " + e.getMessage());
                throw new RuntimeException("Error creating role", e);
            }
        };
    }

    private DataFetcher<Role> updateRoleDataFetcher() {
        return environment -> {
            try {
                String id = environment.getArgument("id");
                String name = environment.getArgument("name");
                String description = environment.getArgument("description");

                Role role = new Role();
                role.setId(Long.parseLong(id));
                role.setName(name);
                role.setDescription(description);
                
                return roleRepository.save(role);
            } catch (Exception e) {
                logger.severe("Error updating role: " + e.getMessage());
                throw new RuntimeException("Error updating role", e);
            }
        };
    }

    private DataFetcher<Boolean> deleteRoleDataFetcher() {
        return environment -> {
            try {
                String id = environment.getArgument("id");
                return roleRepository.deleteById(Long.parseLong(id));
            } catch (Exception e) {
                logger.severe("Error deleting role: " + e.getMessage());
                throw new RuntimeException("Error deleting role", e);
            }
        };
    }

    private DataFetcher<Boolean> assignRoleToUserDataFetcher() {
        return environment -> {
            try {
                String userId = environment.getArgument("userId");
                String roleId = environment.getArgument("roleId");
                roleRepository.assignRoleToUser(Long.parseLong(userId), Long.parseLong(roleId));
                return true;
            } catch (Exception e) {
                logger.severe("Error assigning role to user: " + e.getMessage());
                throw new RuntimeException("Error assigning role to user", e);
            }
        };
    }

    private DataFetcher<Boolean> removeRoleFromUserDataFetcher() {
        return environment -> {
            try {
                String userId = environment.getArgument("userId");
                String roleId = environment.getArgument("roleId");
                roleRepository.removeRoleFromUser(Long.parseLong(userId), Long.parseLong(roleId));
                return true;
            } catch (Exception e) {
                logger.severe("Error removing role from user: " + e.getMessage());
                throw new RuntimeException("Error removing role from user", e);
            }
        };
    }

    public GraphQL getGraphQL() {
        return graphQL;
    }
}