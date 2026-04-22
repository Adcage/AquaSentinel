from marshmallow import Schema, fields


class ResponseEnvelopeSchema(Schema):
    code = fields.String(required=True)
    message = fields.String(required=True)
    request_id = fields.String(required=True)
    data = fields.Raw(required=True)
