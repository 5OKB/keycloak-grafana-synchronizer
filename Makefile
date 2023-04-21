build:
	@echo -e '\e[1;31mBuilding...\e[0m'
	@docker-compose -f docker-compose.dev.yml up grid-build-keycloak-plugin
	@docker cp grid-build-keycloak-plugin:usr/src/mymaven/target ./
	@echo -e '\e[1;31mDone\e[0m'

test:
	@echo -e '\e[1;31mBuilding...\e[0m'
	@docker-compose -f docker-compose.dev.yml up grid-test-keycloak-plugin
	@echo -e '\e[1;31mDone\e[0m'