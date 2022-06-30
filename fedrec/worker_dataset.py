from collections import defaultdict


class WorkerDataset:
    """
    It represents a worker dataset, and includes methods for adding and loading federated workers.

    It also allows for the retrieval of workers based on their role IDs
    ...

    Attributes
    ----------
    _workers: object
        #
    workers_by_types: dict
        #
    _len: int
        #

    Methods
    -------
    add_worker()
        #
    get_worker()
        #
    get_workers_by_roles()
        #
    
    """

    def __init__(self) -> None:
        self._workers = {}
        self.workers_by_types = defaultdict(list)
        self._len = 0

    def add_worker(self,
                   trainer,
                   roles,
                   in_neighbours,
                   out_neighbours):

        in_neighbours = [Neighbour(n) for n in in_neighbours]
        out_neighbours = [Neighbour(n) for n in out_neighbours]

        self._workers[self._len] = FederatedWorker(
            self._len, roles, in_neighbours, out_neighbours, trainer)

        for role in roles:
            self.workers_by_types[role] += [self._len]

        self._len += 1

    def get_worker(self, id):
        # TODO We might persist the state in future
        # So this loading will be dynamic.
        # Then would create a new Federated
        # worker everytime from the persisted storage
        return self._workers[id]

    def get_workers_by_roles(self, role):
        return [self._workers[id] for id in self.get_workers_by_roles[role]]

    def __len__(self):
        return self._len
