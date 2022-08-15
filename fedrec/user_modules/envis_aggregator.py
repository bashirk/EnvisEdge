from fedrec.user_modules.envis_base_module import EnvisBase


class EnvisAggregator(EnvisBase):
    """
    The EnvisAggregator class is in charge of aggregating the data from
    the workers. It is responsible for aggregating the data from the workers,
    into a single dataframe, and then passing it to the model for training.
    
    For example, we can aggregate the results of multiple modules that are
    trained on the same data set, and also the results of those that are
    trained on different data sets.
    -------
    >>> from fedrec.user_modules.envis_aggregator import EnvisAggregator
    >>> from fedrec.user_modules.envis_base_module import EnvisBase
    >>> from fedrec.user_modules.envis_preprocessor import EnvisPreProcessor
    >>> from fedrec.user_modules.envis_trainer import EnvisTrainer
    >>> from fedrec.user_modules.envis_evaluator import EnvisEvaluator
    >>> from fedrec.utilities.logger import BaseLogger
    >>>
    >>> config = {
    >>>     "model": {
    >>>         "name": "LinearRegression",
    >>>         "preproc": {
    >>>             "name": "StandardScaler"
    >>>         }
    >>>     },
    >>>     "log_dir": {
    >>>         "PATH": "./logs/"
    >>>     },
    >>>     "random": {
    >>>         "SEED": 42
    >>>     }
    >>> }
    >>> logger = BaseLogger(config["log_dir"]["PATH"])
    >>> aggregator = EnvisAggregator(config, logger)
    >>>
    >>> # create the modules
    >>> preproc = EnvisPreProcessor(config, logger)
    >>> trainer = EnvisTrainer(config, logger)
    >>> evaluator = EnvisEvaluator(config, logger)
    >>> 
    >>> # aggregate the results
    >>> aggregator.aggregate(preproc, trainer, evaluator)

    Methods
    -------
    __repr__():
        Return a string representation of the object.
    """

    def __init__(self, **kwargs):
        super().__init__(**kwargs)

    def __call__(self, *args, **kwargs):
        raise NotImplementedError('__call__ method not implemented.')

    def __repr__(self):
        return '{}()'.format(self.__class__.__name__)
