from app.api.health import blp


def create_module(app):
    app.extensions["docs_api"].register_blueprint(blp)
