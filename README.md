## Credentials
First of all define your credentials as explained in [AWS documentation](http://docs.aws.amazon.com/AWSSdkDocsJava/latest/DeveloperGuide/credentials.html#using-the-default-credential-provider-chain)

## External jars
- Download the jars listed in the [worker readme](worker/jars/README.md)

## Dev environment
## Local credentials
- In production, machines that are started on EC2 are using `InstanceProfileCredentials`.
If you are running the manager/workers locally, you will need to use regular credentials.
To do so, add the environment variable `DSP_MODE=DEV`. Note that this collides with the [Mocks](#mocks)

### Mocks
- We use [fake_sqs](https://github.com/iain/fake_sqs) to mock AWS SQS,
follow the instruction in the repo to install
(Basically, if you have ruby, `sudo gem install fake_sqs`)
- We use [fakes3](https://github.com/jubos/fake-s3) to mock AWS S3,
follow the instruction in the repo to install
(Basically, if you have ruby, `sudo gem install fakes3`)
- We use [aws-mock](https://github.com/hagai-lvi/aws-mock) to mock AWS EC2,
follow the instruction in the repo to install.
*We currently use a fork because the original repo doesn't have the `InstanceType`s that are using.*
- To use the mocks instead of using AWS, add an environment variable
`DSP_MODE` with the value `DEV-LOCAL`.
If you want to enable only specific mock, use `DSP_MODE_<service-name>=DEV-LOCAL`. For example `DSP_MODE_EC2=DEV-LOCAL`.
**Note** that the mocks support a subset of the actual actions that
are possible in AWS

### Logging
- configure logging in `main/java/resources/log4j.properties`