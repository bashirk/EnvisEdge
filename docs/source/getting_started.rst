******************************
Getting started with EnvisEdge
******************************

.. code:: bash

   git clone https://github.com/NimbleEdge/EnvisEdge

.. code:: bash

   cd EnvisEdge

.. code-block:: RST

   NimbleEdge/EnvisEdge
   ├── CONTRIBUTING.md                         <-- Please go through the contributing guidelines before starting 🤓
   ├── README.md                               <-- You are here 📌
   ├── datasets                                <-- Sample datasets
   ├── docs                                    <-- Tutorials and walkthroughs 🧐
   ├── experiments                             <-- Recommendation models used by our services
   └── fedrec                                  <-- Whole magic takes place here 😜 
         ├── communication_interfaces              <-- Modules for communication interfaces eg. Kafka
         ├── data_models                           <-- All data modules that will be used for communication and thier serializers and  deserializers
         ├── modules                               <-- All the modules related to transformers, embeddings etc.
         ├── multiprocessing                       <-- Modules to run parallel worker jobs
         ├── optimization                          <-- Modules realted to torch optimizers and gradient decesnt etc.
         ├── python_executors                      <-- Contains worker modules eg. trainer and aggregator
         ├── serialization                         <-- serialization interfaces for data models
         ├── user_modules                          <-- Envis modules for wrapping toech modules for users. 
         └── utilities                             <-- Helper modules
   ├── fl_strategies                           <-- Federated learning algorithms for our services.
   ├── notebooks                               <-- Jupyter Notebook examples
   ├── scala-core                              <-- Backbone of EnvisEdge
   ├── scripts                                 <-- bash scripts for creating and removing kfka topics.
   └── tests                                   <-- tests

Update the config files of the model (can be found
`here <https://github.com/NimbleEdge/EnvisEdge/tree/main/configs>`__)
you are going to use as your logging directory:

.. code::

   log_dir:
     PATH: <path to your desired logging directory>

Download kafka from
`Here <https://www.apache.org/dyn/closer.cgi?path=/kafka/3.1.0/kafka_2.13-3.1.0.tgz>`__
👈 and follow these next steps to start the kafka server

Create a ``kafka`` directory. This will be the folder where ``kafka`` will be installed:

.. code:: bash

   mkdir ~/kafka 

Then ``cd`` to the directory
.. code:: bash

   cd ~/kafka

Next, extract and unzip the kafka file archive

.. code:: bash

   tar -xvzf ~/<path>/<to>/<Downloaded>/kafka_2.13-3.1.0.tgz -C ~/kafka

   tar -xvzf ~/Downloads/kafka_2.13-3.1.0.tgz -C ~/kafka  #author's download path

Then ``cd`` back into the directory for the new file folder
.. code:: bash

   cd ~/kafka_2.13-3.1.0

Optionally, rename the folder by running
.. code:: bash

   mv kafka_2.13-3.1.0 kafka #run command with sudo if the shell raises permission error

For Mac: You can follow the instructions in this guide to setup Kafka on your Mac environment.
`Here <https://hevodata.com/learn/install-kafka-on-mac/>`__
👈

Now, run the following command on your unix (Mac/Linux) environment to run zookeeper

.. code:: bash

   bin/zookeeper-server-start.sh config/zookeeper.properties

Then, run this command to start the Kafka environment

.. code:: bash

   bin/kafka-server-start.sh config/server.properties

Create ``kafka`` topics for the job executor

.. code:: bash

   cd scripts
   $ bash create_topics.sh
   Enter path to kafka Directory : <Enter the path to the kafka directory>
   kafka url: <Enter the URL on which kafka is listening e.g if you are running it on localhost it would be 127.0.0.1>
   Creating Topics...

Before installing the dependencies needed for this project

``cd`` back into the `EnvisEdge` project directory
.. code:: bash

   cd ~/path/to/EnvisEdge

If you don't have the `virtualenv` package, you can run the command below to do the installation before proceeding

.. code:: bash

   pip install virtualenv

Now, install the dependencies using virtual environment

.. code:: bash

   mkdir env
   cd env
   virtualenv envisedge
   source envisedge/bin/activate
   pip3 install -r /path/to/requirements.txt

Download the federated dataset

.. code:: bash

   $ bash download.sh -f
   Enter global data path : <Enter the path you want your dataset to be saved>
   Enter model : <Enter the config file of the model to update with the dataset path>
   Downloading femnist dataset...

Run data preprocessing with `preprocess_data <https://github.com/NimbleEdge/EnvisEdge/blob/main/preprocess_data.py>`__ .
Using this dataset, you will prepare a client_id mapping in the dataset
that will be sent to Python workers for training the model.

.. code:: bash

   python preprocess_data.py --config configs/regression.yml

To start the multiprocessing executor, run the following command:

.. code:: bash

   $ python executor.py --config configs/regression.yml

To see how traning is done, run the following command:

.. code:: bash

   $ python tests/integration_tests/integration_test.py --config configs/regression.yml
