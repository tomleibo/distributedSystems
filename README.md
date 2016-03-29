## Credentials
First of all define your credentials as explained in [AWS documentation](http://docs.aws.amazon.com/AWSSdkDocsJava/latest/DeveloperGuide/credentials.html#using-the-default-credential-provider-chain)

## Dev environment
### Mocks
- We use [fake_sqs](https://github.com/iain/fake_sqs) to mock AWS SQS,
follow the instruction in the repo to install
(Basically, if you have ruby, `sudo gem install fake_sqs`)
- We use [fakes3](https://github.com/jubos/fake-s3) to mock AWS S3,
follow the instruction in the repo to install
(Basically, if you have ruby, `sudo gem install fakes3`)
- To use the mocks instead of using AWS, add an environment variable
`DSP_MODE` with the value `DEV`
**Note** that the mocks support a subset of the actual actions that
are possible in AWS

### Logging
- configure logging in `main/java/resources/log4j.properties`