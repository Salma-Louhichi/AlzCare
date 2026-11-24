// Dev-server proxy. Override the targets when the default ports are taken on your machine:
//   GATEWAY_URL=http://localhost:8081 npm start
const gateway = process.env.GATEWAY_URL || 'http://localhost:8080';
const keycloak = process.env.KEYCLOAK_URL || 'http://localhost:8090';

module.exports = {
  '/api': { target: gateway, secure: false, changeOrigin: true, logLevel: 'warn' },
  '/realms': { target: keycloak, secure: false, changeOrigin: true, logLevel: 'warn' },
  '/resources': { target: keycloak, secure: false, changeOrigin: true, logLevel: 'warn' },
};
