build:
	@echo -e '\e[1;31mBuilding...\e[0m'
	@docker-compose -f docker-compose.dev.yml run --rm grid-keycloak-plugin-wait-for-dependencies
	@docker-compose -f docker-compose.dev.yml up grid-keycloak-plugin-build
	@docker cp grid-keycloak-plugin-build:usr/src/mymaven/target ./
	@docker-compose -f docker-compose.dev.yml stop
	@echo -e '\e[1;31mDone\e[0m'

test:
	@echo -e '\e[1;31mBuilding...\e[0m'
	@docker-compose -f docker-compose.dev.yml run --rm grid-keycloak-plugin-wait-for-dependencies
	@docker-compose -f docker-compose.dev.yml up grid-keycloak-plugin-test
	@docker-compose -f docker-compose.dev.yml stop
	@echo -e '\e[1;31mDone\e[0m'