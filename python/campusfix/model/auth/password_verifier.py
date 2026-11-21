from hashlib import pbkdf2_hmac
from hmac import compare_digest


_ITERATIONS = 600_000
_HASH_LENGTH = 32


def matches(password: str, salt: bytes, password_hash: bytes) -> bool:
    derived_key = pbkdf2_hmac("sha256", password.encode(), salt, _ITERATIONS, _HASH_LENGTH)
    return compare_digest(password_hash, derived_key)
