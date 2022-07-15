from abc import ABC, abstractmethod
from json import dumps, loads

from fedrec.utilities import registry


class SerializationStrategy(registry.Registrable, ABC):
    """"
    This class is an abstract class for serialization strategies. It inherits
    from the Registrable class to allow for registration of serialization
    strategies.
    
    Methods
    --------
    parse(obj):
        Parses an object into a dictionary.
    unparse(obj):
        Unparses a dictionary into an object.
    """

    @abstractmethod
    def parse(self, obj):
        raise NotImplementedError()

    @abstractmethod
    def unparse(self, obj):
        raise NotImplementedError()


@registry.load("serialization", "json")
class JSONSerialization(SerializationStrategy):
    """Uses json serialization strategy for objects.

    Attributes
    ----------
    serializer: str
        The serializer to use.
    """

    def parse(self, obj):
        """Serializes a python object to json.

        Parameters
        -----------
        obj: object
            The object to serialize.
        Returns
        --------
        str
        """
        return loads(obj)

    def unparse(self, obj):
        """Deserializes the json object to python object
         as per the `type` mentioned in the json dictionary.

        Parameters
        -----------
        obj: object
            The object to deserialize.
        Returns
        --------
        object
        """
        return dumps(obj, indent=4)
