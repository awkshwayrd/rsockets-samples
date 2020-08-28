# rsockets-samples
Sample code trying out RSockets applications

1. The *client*, *server*, and *integration* dirs contain code walk-through from Josh Long's [The RSocket Revolution](https://www.youtube.com/watch?v=ipVfRdl5SP0) talk.

   1. For plain server, command line client https://github.com/making/rsc was downloaded and used.

      `rsc tcp://localhost:8888 --stream --route greetings --log --debug -d "{ \"name\" : \"Josh\" }"`

   2. For integration, the following commands was executed

      `cd ~/Desktop/in && echo "Spring" > name`

2. 

