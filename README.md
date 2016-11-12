**Open Source Projects Search Engine Using ElasticSearch**


THIS PROJECT DEPENDS ON A UNIX SYSTEM!




**Project Description:**

This project is a search engine for searching open source projects based on some provided criteria by the user.
The user can search for specific keywords in project id, project name, project description, project tags and project
source code.  These projects are retrieved from OpenHub and indexed into ElasticSearch that is deployed on a Google
Cloud Compute Engine. It’s IP Address is 104.197.88.210 .  The web service is also deployed on another Compute Engine
instance with an IP Address of 104.154.133.99 and uses port 9999 .


This project includes two packages ElasticIndexer and MyServer.


ElasticIndexer package contains logic that queries projects from OpenHub using the Ohloh API. It then parses the
response to extract only git repository urls, because the project uses GitHub API to clone the repositories onto the
machine.  Once the git repositories are cloned it indexes all the necessary information into ElasticSearch that is
deployed on a Google Compute Engine. Before indexing the files however it filters out all of the common language
reserved words.  This is all executed by running the OhlohMain object.  This part needs to be run before you start the
web server that interfaces with ElasticSearch if the index for ElasticSearch has not been created.  For our purposes
however this has already been done and ElasticSearch already is indexed with all the necessary open source project
information.


MyServer package contains logic that interfaces with ElasticSearch and responds to user requests.

The user can send requests to the web service using the endpoint:

http://104.154.133.99:9999/elastic?keyword=java&where=code&results=100

“where” accepts values of id, project_name, description, url, tags and code.

“keyword” accepts any word.

“results” accepts any number value.

If “where” has value of “code” then it will look through all of the indexed files and will return a list of all files
that contained the keyword along with the path of the file and the project that it belongs to.  If “where” has a value
other than “code” then it returns the meta-data of all the projects that matched the query.




**How to run and test:**


To build and run the project do ‘sbt build’ and ‘sbt run’ inside the project directory.  It will automatically only
run the MyServer.Server and not ElasticIndexer.OhlohMain.


To query the web service you can do curl -XGET to the endpoint of the service or just click on the urls:

curl -XGET http://104.154.133.99:9999/elastic?keyword=engine&where=code

curl -XGET http://104.154.133.99:9999/elastic?keyword=variables&where=code

curl -XGET http://104.154.133.99:9999/elastic?keyword=php&where=tags

curl -XGET http://104.154.133.99:9999/elastic?keyword=library&where=code&results=100

curl -XGET http://104.154.133.99:9999/elastic?keyword=java&where=description

curl -XGET http://104.154.133.99:9999/elastic?keyword=c++&where=description

curl -XGET http://104.154.133.99:9999/elastic?keyword=python&where=tags

curl -XGET http://104.154.133.99:9999/elastic?keyword=library&where=project_name




**SoapUI Load Test Results:**


There were 7 different types of requests that the load tests were executing each one to search for different keywords
and in different places.


**Test 1** : with simulating a 100 users that spawn requests every 300ms with randomization of 0.5

Transactions/Sec = 48.43

Bytes processed = 86,845,180

Bytes processed per second = 2,880,819

===========================================================================================

**Test 2** : with simulating a 100 users that spawn requests every 50ms with randomization of 0.5

Transactions/Sec = 56.32

Bytes processed = 100,764,202

Bytes processed per second = 3,350,207

===========================================================================================

**Test 3** : with simulating 200 users that spawn requests every 300 with randomization of 0.5

Transactions/Sec = 52.56

Bytes processed = 94,577,970

Bytes processed per second = 3,126,957

===========================================================================================

**Test 4** : with simulating 400 users that spawn requests every 300ms with randomization of 0.5

Transactions/Sec = 50.28

Bytes processed = 90,771,058

Bytes processed per second = 2,991,203

===========================================================================================

**Test 5** : with simulating 800 users that spawn requests every 300ms with randomization of 0.5

Transactions/Sec = 48.2

Bytes processed = 86,845,180

Bytes processed per second = 2,867,502