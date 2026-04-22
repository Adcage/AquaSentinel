from flask import current_app
from sqlalchemy import create_engine, func
from sqlalchemy.orm import DeclarativeBase, scoped_session, sessionmaker
from sqlalchemy.pool import StaticPool


class Base(DeclarativeBase):
    pass


class Database:
    Model = Base
    func = func

    def init_app(self, app):
        uri = app.config["SQLALCHEMY_DATABASE_URI"]

        engine_options: dict = {"future": True}
        if uri == "sqlite:///:memory:":
            engine_options["connect_args"] = {"check_same_thread": False}
            engine_options["poolclass"] = StaticPool

        engine = create_engine(uri, **engine_options)
        session_factory = scoped_session(
            sessionmaker(bind=engine, autoflush=False, expire_on_commit=False)
        )

        app.extensions["db_engine"] = engine
        app.extensions["db_session_factory"] = session_factory

        @app.teardown_appcontext
        def remove_session(_exception=None):
            session_factory.remove()

    @property
    def session(self):
        session_factory = current_app.extensions["db_session_factory"]
        return session_factory()

    def create_all(self):
        engine = current_app.extensions["db_engine"]
        self.Model.metadata.create_all(bind=engine)

    def drop_all(self):
        engine = current_app.extensions["db_engine"]
        self.Model.metadata.drop_all(bind=engine)


db = Database()
