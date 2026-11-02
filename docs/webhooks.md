# Webhooks

Webhooks tell your server when something happens to a payment, so you don't have to poll the API.

## Registering an endpoint

Add an HTTPS endpoint under **Settings → Developers → Webhooks** and choose the events to receive. Each endpoint
gets its own signing secret, shown once when it is created. Endpoints must answer with a `2xx` status within
10 seconds.

## Events

| Event | Sent when |
| --- | --- |
| `payment.succeeded` | A payment's money has been captured |
| `payment.failed` | A payment was declined or expired |
| `refund.created` | A refund was started |
| `refund.succeeded` | A refund reached the customer |
| `dispute.opened` | The customer's bank opened a chargeback |

Each event has an `id`, a `type`, a `created` timestamp and a `data` object with the payment or refund.

## Verifying signatures

Every webhook carries a `Northwind-Signature` header of the form `t=<timestamp>,v1=<signature>`. To verify it,
compute an HMAC-SHA256 of `<timestamp>.<raw request body>` with the endpoint's signing secret and compare it with
`v1` using a constant-time comparison. Reject the request if the timestamp is more than five minutes old, which
stops replayed requests. Always verify against the raw body bytes; parsing and re-serialising the JSON changes
them and the signature will not match.

## Retries and duplicates

If your endpoint does not return `2xx`, the event is retried with exponential backoff for up to three days:
after 1 minute, 5 minutes, 30 minutes, 2 hours, and then every 6 hours. Because of retries the same event can
arrive more than once. Store each processed event `id` and skip events you have already handled, so processing
is idempotent.

## Ordering

Events are not guaranteed to arrive in order. A `payment.succeeded` event can arrive before the
`payment.processing` one. Read the current status from `data.status` or fetch the payment with
`GET /v1/payments/{id}` instead of relying on the order of events.
