// Publique a Action e conecte-a ao fluxo Post Login do seu Auth0.
exports.onExecutePostLogin = async (event, api) => {
  const ns = 'https://social-insper.com/';
  api.accessToken.setCustomClaim(`${ns}email`, event.user.email);
  api.accessToken.setCustomClaim(`${ns}roles`, event.authorization?.roles || []);
};
