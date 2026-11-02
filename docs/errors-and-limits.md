# Errors, rate limits and pagination

## Error format

Errors return a non-`2xx` status and a JSON body:

```json
{ "error": { "type": "invalid_request", "code": "amount_too_small", "message": "Amount must be at least 100.", "param": "amount" } }
```

| Status | Type | Meaning | Retry? |
| --- | --- | --- | --- |
| 400 | `invalid_request` | A parameter is missing or invalid | No: fix the request |
| 401 | `authentication_error` | The API key is missing, wrong or revoked | No |
| 402 | `card_error` | The card was declined; `code` says why | No: ask for another card |
| 409 | `conflict` | Idempotency key reused with a different body | No |
| 429 | `rate_limit_error` | Too many requests | Yes, after the `Retry-After` delay |
| 500, 503 | `api_error` | Problem on Northwind's side | Yes, with backoff and the same idempotency key |

## Rate limits

Live keys may send 100 requests per second; sandbox keys 25. Limits are per account, not per key. When you go
over, the API returns `429` with a `Retry-After` header giving the number of seconds to wait. Every response
includes `RateLimit-Remaining` so you can slow down before hitting the limit.

## Pagination

List endpoints such as `GET /v1/payments` return at most 100 items. Pass `limit` (1 to 100, default 20) and, to
get the next page, `starting_after` with the `id` of the last item you received. The response field `has_more`
is `false` on the last page. Cursor pagination stays correct while new payments are being created, which offset
pagination would not.
