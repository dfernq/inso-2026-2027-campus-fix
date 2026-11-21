from typing import Protocol, TypeVar


T = TypeVar("T")


class CrudDao(Protocol[T]):
    def create(self, entity: T) -> T:
        """Insert an entity and return it with its generated ID."""
        ...

    def read(self, entity_id: int) -> T | None:
        """Return the entity with the given ID, if present."""
        ...

    def update(self, entity: T) -> bool:
        """Update the entity by ID and report whether it was found."""
        ...

    def delete(self, entity_id: int) -> bool:
        """Delete the entity by ID and report whether it was found."""
        ...
