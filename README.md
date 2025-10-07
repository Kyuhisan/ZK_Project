# Project Instructions:
## Building Backend:
- Clean Maven lifecycle and Package into JAR.
- Run ```docker build -t <dockerHubName>/<imageName>-backend:<version> .```
- Run ```docker push <dockerHubName>/<imageName>-backend:<version>```

## Building Frontend:
- Run ```docker build -t <dockerHubName>/<imageName>-frontend:<version> .```
- Run ```docker push <dockerHubName>/<imageName>-frontend:<version>```

## Running Backend:
- Download ```.env``` file from shared directory (OneDrive).
- Save ```.env``` in ```/02_Backend/``` directory.
- Modify ```.env``` file with custom configuration (API, Passwords, Keys).
- Run ```docker-compose up -d``` inside ```/02_Backend/``` directory.

## Running Frontend:
- Run ```docker run -d -p <port>:80 -e API_URL="<Backend IP>:<PORT>" <dockerHubName>/<imageName>-frontend:<version>``` inside ```/01_Frontend/``` directory.

## Establishing Vagrant environment:
- Open administrator powershell or other form of terminal.
- Move into ```/03_Vagrant/``` directory.
- Execute initial Vagrant setup with ```vagrant up --provider=hyperv``` or if HyperV is not available use alternatives.
- During setup, you will have to set permissions, which switch Vagrant will use for networking ```WSL (Hyper-V firewall)``` and log in using device credentials.
- Once setup is finished, environment can be accessed using ```vagrant ssh``` and files can be shared through ```/Shared``` directory, created by setup.
  
## Running SmartSearch inside Vagrant environment:
- Start up vagrant using ```vagrant up --provider=hyperv```.
- Enter vagrant environment using ```vagrant ssh```.
- Use ```/Shared``` directory to forward ```.env and docker-compose.yml``` files or if needed entire git repository.
- Run SmartSearch Backend container using ```docker-compose up -d```.
- Run SmartSearch Frontend container using ```docker run -d -p <Frontend Port>:80 -e API_URL="Vagrant IP":<Backend Port>" matickuhar/smartsearch-frontend:<version>``` 
- To check on which IP Vagrant is running use ```ip addr show```.
- When done, environement can be stopped using ```vagrant halt``` or completely removed using ```Vagrant destroy```.

## Running SmartSearch in production:
- Download ```docker-compose.prod.yml``` file.
- Run backend with ```docker-compose -f docker-compose.prod.yml up -d```.
- Run frontend with ```docker run -d -p <Frontend Port>:80 -e API_URL="Backend IP":<Backend Port>" matickuhar/smartsearch-frontend:5.0``` 

## Resources:
Official docker-hub repositories and used dependencies.
- https://hub.docker.com/r/matickuhar/smartsearch-backend
- https://hub.docker.com/r/matickuhar/smartsearch-frontend
- https://www.selenium.dev/documentation/selenium_manager
