#JWT Token generator

This will generate tokens bases on the user-profiles.mock. It will take the roles of every profile and sign it with a custom RS512 key. 
These tokens will be use in the application to set the permissions of the users. After you changed the roles in the file, you have to generate new tokens to affect the application.

## Generate new tokens
To generate new tokens run the npm script "npm run generate-tokens".  
