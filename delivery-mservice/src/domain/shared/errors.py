class DomainError(Exception):
    pass


class InvalidStateTransition(DomainError):
    pass


class InvalidValueObject(DomainError):
    pass


class BusinessRuleViolation(DomainError):
    pass


class NotFound(DomainError):
    pass
