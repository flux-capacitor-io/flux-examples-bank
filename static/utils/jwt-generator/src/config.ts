const {generateKeyPairSync} = require('crypto');

const ecKeyPairOptions = {
  modulusLength: 4096,
  publicKeyEncoding: {
    type: 'pkcs1',
    format: 'pem'
  },
  privateKeyEncoding: {
    type: 'pkcs8',
    format: 'pem'
  }
};

const key = generateKeyPairSync('rsa', ecKeyPairOptions);

export default {
  "secret": key.privateKey,
  "signOptions": { "algorithm": "RS512" }
}
