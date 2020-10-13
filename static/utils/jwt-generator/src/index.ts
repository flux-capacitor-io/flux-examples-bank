import {createWriteStream} from 'fs';
import config from "./config";
import {mockProfiles} from "../mocks/user-profiles.mock";
import {sign} from "jsonwebtoken"

const usersAndTokens = {};

const signToken = (profile) => {
  const token = sign(profile, config.secret, { "algorithm": "RS512" });
  const profileName = profile.sub;
  return { profileName, token };
};

for (let mockProfile in mockProfiles) {
  const userAndToken = signToken(mockProfiles[mockProfile]);
  usersAndTokens[userAndToken.profileName] = userAndToken.token
}

createWriteStream(__dirname + "/tokens.ts").write("export default " + JSON.stringify(usersAndTokens), (error => {
  if (error) {
    console.log("\x1b[31m%s\x1b[0m", error)
  } else {
    console.log("\x1b[32m%s\x1b[0m", "JWT Tokens generated");
  }
}));





