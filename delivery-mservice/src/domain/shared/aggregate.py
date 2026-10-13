class AggregateRoot:
    def __init__(self):
        self._events = []

    def record_event(self, event):
        self._events.append(event)

    def pull_events(self):
        events = self._events[:]
        self._events.clear()
        return events
