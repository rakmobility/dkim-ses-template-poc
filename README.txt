##This project contains sample Java program to send mail through AWS SES with DKIM signing

---

1. Create 2048 private key using the below command

`openssl genrsa -des3 -out private.pem 2048`

2. Create public key using the below command for the private key generated.
`openssl rsa -in private.pem -outform PEM -pubout -out public.pem`

3. Add the public key generated(as mentioned below) to the DNS Configuration

Name                               Type           Value
selector._domainkey.example.com    TXT            p=yourPublicKey

Note: 	Replace selector with a unique name that identifies the key. Replace example.com with your domain.
 		Replace yourPublicKey with the public key generated. Delete the first and last lines (-----BEGIN PUBLIC KEY----- and -----END PUBLIC KEY-----, respectively) of the generated public key in  
 		point 2.  Split the public key into multiple strings using an online tool.

4. Create encrypted private key for the above generated private key using  the below command. This encrypted private key path to be used in AmazonSESDKIMSIGN.java
`openssl pkcs8 -topk8 -nocrypt -in private.pem -out private.key.der -outform der`

5. Create a new SES template from AWS Cli 

`aws ses create-template --cli-input-json file:///path/template.json`

5. Make changes to AmazonSESDKIMSIGN.java as mentioned in TODO comments and run AmazonSESDKIMSIGN.java as Java Application in IDE.
