from campusfix import desktop_database
from campusfix.controller.campusfix_controller import CampusFixController
from campusfix.model.facade import CampusFix


def main() -> None:
    try:
        database = desktop_database.get_instance()
    except ValueError as error:
        raise SystemExit(str(error)) from None

    model = CampusFix(database)
    CampusFixController(model).start()


if __name__ == "__main__":
    main()
