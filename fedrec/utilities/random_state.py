import random
import sys
import attr
from typing import Dict
import numpy as np
import torch


class RandomState:
    """
    The RandomState represents the random state, and includes a method for setting and restoring states.
    It gets the state for the generator, Numpy, and the Random Number State generators (RNGs) modules.

    ...

    Attributes
    ----------
    random_mod_state: object
        the current state of the generator
    np_state: tuple
        the current state of the internal generator
    torch_cpu_state: tuple
        the state of the random number state generator of the CPU
    torch_gpu_states: list
        list containing the state of the random number state generator of the GPU for each device 

    Method
    -------
    restore()
        sets and restores the state for Numpy, Torch, & Torch RNGs
    
    """

    def __init__(self):
        self.random_mod_state = random.getstate()
        self.np_state = np.random.get_state()
        self.torch_cpu_state = torch.get_rng_state()
        self.torch_gpu_states = [
            torch.cuda.get_rng_state(d)
            for d in range(torch.cuda.device_count())
        ]

    def restore(self):
        random.setstate(self.random_mod_state)
        np.random.set_state(self.np_state)
        torch.set_rng_state(self.torch_cpu_state)
        for d, state in enumerate(self.torch_gpu_states):
            torch.cuda.set_rng_state(state, d)


class RandomContext:
    """
    The RandomContext represents the context of the random state that saves the state of the RNGs.
    
    It sets the state for the RNGs by using a random integer seed value, then goes ahead to set and restore the state for the RNGs. 
    The RandomContext class uses a context manager to activate the random context if it is inactive, which ensures that the values 
    are set only one time. It also uses context managers to handle the runtime context during code execution.
    
    ...

    Arguments
    ----------
    seed: optional
        The seed value to use for the generation of the random number (default is None). Useful when seed is called in order to reset the RNG which makes the random numbers predictable

    Attributes
    ----------
    outside_state: array
        sets the state of the generator
    inside_state: array
        sets the state of the internal generator, 
    _active: bool
        set the active state of the RandomContext to False

    Methods
    -------
    __enter__()
        context manager for the RandomContext to enter the context of the runtime. 
        It returns the saved state of the RNG, and sets the RandomContext to active only when it is already inactive
    __exit__()
        context manager for the RandomContext to exit the context of the runtime. It includes arguments
        that are descriptions of the exceptions that caused the context exit
    
    """

    def __init__(self, seed=None):
        outside_state = RandomState()

        #initialize the random number generator and make it reproducible
        random.seed(seed)
        #make the random number generator for the numpy module reproducible, by setting the seed value
        np.random.seed(seed)
        #initialize the random number generator (RNG) for the torch module, with the seed value being a random integer
        if seed is None:
            torch.manual_seed(random.randint(-sys.maxsize - 1, sys.maxsize))
        else:
            torch.manual_seed(seed)
        # torch.cuda.manual_seed_all is called by torch.manual_seed
        self.inside_state = RandomState()

        outside_state.restore()

        self._active = False

    def __enter__(self):
         """It returns the saved state of the RNG, and sets the RandomContext to active only when it is already inactive"""
        if self._active:
            raise Exception('RandomContext can be active only once')

        # Save current state of RNG
        self.outside_state = RandomState()
        # Restore saved state of RNG for this context
        self.inside_state.restore()
        self._active = True

    def __exit__(self, exception_type, exception_value, traceback):
        """It saves and restores the current state of the RNG, upon exiting the runtime, then proceeds to set the RandomContext to inactivate"""
        # Save current state of RNG
        self.inside_state = RandomState()
        # Restore state of RNG saved in __enter__
        self.outside_state.restore()
        self.outside_state = None

        self._active = False


@attr.s
class RandomizationConfig:
    # Seed for RNG used in shuffling the training data.
    data_seed = attr.ib(default=None)
    # Seed for RNG used in initializing the model.
    init_seed = attr.ib(default=None)
    # Seed for RNG used in computing the model's training loss.
    # Only relevant with internal randomness in the model, e.g. with dropout.
    model_seed = attr.ib(default=None)


class Reproducible(object):
    def __init__(self, config: Dict) -> None:
        self.data_random = RandomContext(
            config["data_seed"])
        self.model_random = RandomContext(
            config["model_seed"])
        self.init_random = RandomContext(
            config["init_seed"])
