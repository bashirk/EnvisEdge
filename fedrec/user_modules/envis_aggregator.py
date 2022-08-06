from fedrec.user_modules.envis_base_module import EnvisBase


class EnvisAggregator(EnvisBase):
    """
    The EnvisAggregator class is in charge of aggregating the data from
    the workers. It is used to aggregate the results of multiple EnvisBase
    modules into a single result.
    
    For example, we can aggregate the results of multiple modules that are
    trained on the same data set, and also the results of those that are
    trained on different data sets.

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
