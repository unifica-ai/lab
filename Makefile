test:
	circleci local execute -c <(circleci config process .circleci/config.yml) test
