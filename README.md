# Project02_VehiclesJDBCspringBoot

Spring Boot + JDBC version of the vehicle registry of
**[Project01_Vehicles](https://github.com/ersmax/VehicleBase)**, which kept the
vehicles in an in-memory list and saved them as plain JSON.
The program reads text commands from a file, validates them and stores the
vehicles (cars, motorbikes, bicycles) in a **PostgreSQL** database through
**Spring JDBC** (`NamedParameterJdbcTemplate`). Vehicles can be added, updated,
deleted and listed with filters; every `list` writes its result into an output file.

The structure follows the example project **L016_sqljdbc**: an application class
with a `CommandLineRunner`, Spring components that hold the logic, DAOs
(`@Repository`) that run the SQL, and Lombok models.

---

## From Project01 to Project02: what changed

| Topic | [Project01](https://github.com/ersmax/VehicleBase) (plain Java + JSON) | Project02 (Spring Boot + JDBC) |
|-------|-------------------|---------------------------|
| Start | `MainVehicles.main` loads `StartVehicle` with `Class.forName` | `VehiclesJdbCspringBootApplication` + `CommandLineRunner`; Spring creates and injects `StartVehicle` |
| Objects | created with `new` / `newInstance()` | Spring beans (`@Component`, `@Repository`), injected with `@RequiredArgsConstructor` + `private final` fields |
| Data store | `VehicleRegistry` singleton (in-memory list + plate index), lost at the end of the run | PostgreSQL tables, accessed through DAOs |
| Persistence | `export` / `import` of the whole list to `data/vehicles.json` (Jackson, `@JsonTypeInfo` + `@JsonSubTypes`) | every operation is saved in the database immediately: no export / import needed |
| Ids | counter in the singleton (`nextId++`) | `identity` column, returned by the insert (`KeyHolder`) |
| Plate uniqueness | `Map<String, Vehicles>` plate index in the singleton | `unique` constraint per table + `VehiclesDAO.findIdByPlate` across cars and bikes |
| Allowed values (fuel, color, ...) | `List.of(...)` constants; `brand` free text | lookup tables (brands included), read by `LookupDAO` |
| Filters of `list` | `Predicate<Vehicles>` built with `.and(...)` and applied with `stream().filter()` | dynamic SQL `where` built by `VehiclesDAO.findByFilters` |
| Vehicle service lookup | `Class.forName("services.CarImplementation")` + `vehicleCache` | `context.getBean("carImplementation")` (beans are already singletons, no cache) |
| Operation lookup | `getMethod` + `methodsCache` | `getMethod` (no cache) |
| Operations | `add`, `delete`, `list`, `export`, `import` | `add`, `update`, `delete`, `list` |
| Bicycle `suspension` | text: `with` / `without` | boolean: `yes` / `no` |
| Bicycle `gears` | 1-100 | 1-50 (also optional for car and bike, 1-10) |
| Several tables changed together | — | `@Transactional` (all or nothing) |

---

## Technologies

| Technology | Use |
|------------|-----|
| Java 25 | language (records, switch expressions, pattern matching `instanceof`, text blocks) |
| Spring Boot 4.1.2 | application start, dependency injection, configuration |
| Spring JDBC | `NamedParameterJdbcTemplate`, `MapSqlParameterSource`, `KeyHolder`, `@Transactional` |
| PostgreSQL | database (`postgresql` JDBC driver) |
| Lombok | `@Getter`, `@Setter`, `@ToString`, `@SuperBuilder`, `@NoArgsConstructor`, `@RequiredArgsConstructor`, `@Slf4j` |
| SLF4J / Logback | logging (configured in `application.properties`) |

---

## How to run

1. Start PostgreSQL and create the database `vehiclesTv`.
2. Run `script.sql` on it (DBeaver or psql): it drops and recreates all tables
   and fills the lookup tables. **It deletes all existing data.**
3. Check the connection in `src/main/resources/application.properties`:

       spring.datasource.url = jdbc:postgresql://localhost:5432/vehiclesTv
       spring.datasource.username=postgres
       spring.datasource.password=password

4. Write the commands in `data/readData.txt`.
5. Run `VehiclesJdbCspringBootApplication` (Run As > Spring Boot App).
6. Results: the console log (added / updated / deleted / errors, generated SQL)
   and `data/writeData.txt` (result of every `list`).

The paths in `data/` are relative to the project root, so the program must be
started from the project folder (default in Eclipse).

---

## Files

| File | Role |
|------|------|
| `script.sql` | creates the database structure and the lookup values |
| `data/readData.txt` | input: one command per line (blank lines and lines starting with `/` are ignored) |
| `data/writeData.txt` | output of every `list` command (emptied at the start of each run) |
| `src/main/resources/application.properties` | database connection and logging |

---

## Input format

    add,<type>,key=value,key=value,...
    update,id=<id>,key=value,...          |   update,plate=<plate>,key=value,...
    delete,id=<id>                        |   delete,plate=<plate>
    list                                  |   list,key=value,key=value,...

- Values are separated by `,` and every parameter is `key=value`.
- Keys are case-insensitive (they are converted to lowercase).
- `add` needs the vehicle type: `car`, `bike`, `bicycle`.
- `update` and `delete` need no type: the vehicle is found by `id` (any vehicle)
  or by `plate` (car, bike), and its type is read from the database.
- `update` contains **only the fields to change**: the other values are taken
  from the database (see "Update" below). The type of a vehicle cannot change.

Examples:

    add,car,wheels=4,fuel=petrol,category=road,color=white,brand=fiat,year=2025,model=500,doors=4,plate=el234gx,cc=1200
    add,bike,wheels=2,fuel=petrol,category=road,color=black,brand=Yamaha,year=2025,model=r1,plate=EL22239,cc=900
    add,bicycle,wheels=2,fuel=manual,category=road,color=black,brand=Bianchi,year=2025,model=Grizl 5,gears=10,suspension=no,foldable=no
    update,plate=FL234GX,color=red,cc=1400
    update,id=3,color=white
    delete,plate=el234gx
    delete,id=4
    list
    list,type=car,color=white

### Keys

| Key | Field | Vehicles | Allowed values |
|-----|-------|----------|----------------|
| `wheels` | `numberWheels` | all | number (car >= 3, bike and bicycle = 2) |
| `fuel` | `fuelType` | all | values of table `fuel_types` (car and bike not `manual`, bicycle only `manual` or `electric`) |
| `category` | `category` | all | values of table `categories` |
| `color` | `color` | all | values of table `colors` |
| `brand` | `brand` | all | values of table `brand_types` |
| `model` | `model` | all | text |
| `year` | `productionYear` | all | number, last 50 years |
| `gears` | `numberGears` | all | car and bike: optional, 1-10; bicycle: required, 1-50 |
| `plate` | `plate` | car, bike | car `AA123AA`, bike `AA12345`, unique across cars and bikes |
| `cc` | `cc` | car, bike | car 50-8000, bike 50-3000 |
| `doors` | `numberDoors` | car | 2-7 |
| `suspension` | `suspensions` | bicycle | `yes` / `no` (also `true` / `false`) |
| `foldable` | `foldable` | bicycle | `yes` / `no` (also `true` / `false`) |

The keys are the names used in the file, not the Java field names: the mapping
key -> field is written in `VehicleAbstract.fillProperties` and in the `build`
method of each implementation.

---

## Packages

All packages are under `com.betacom.vehicles`, the package of the application
class: Spring scans only this package and its sub-packages
(`@SpringBootApplication`).

| Package | Classes | Role |
|---------|---------|------|
| (root) | `VehiclesJdbCspringBootApplication` | entry point; file paths; `CommandLineRunner` that calls `StartVehicle.execute()` |
| `process` | `StartVehicle` (`@Component`) | clears the output file, reads the input, dispatches each row to its operation **by reflection** |
| | `StringParsing` | splits a row on `,` and turns `key=value` items into a `Map` (streams) |
| `services` | `ListImplementation` (`@Component`) | one public method per operation: `addVehicle`, `updateVehicle`, `deleteVehicle`, `listVehicles`; loads the vehicle service **by name** and calls it **by reflection** |
| | `VehicleOperations` (interface) | `add`, `update`, `delete`, same signature for every vehicle type |
| | `VehicleAbstract` | implements `VehicleOperations`; validation of the common attributes (`fillProperties`) |
| | `CarImplementation`, `BikeImplementation`, `BicycleImplementation` (`@Component`) | type-specific validation and saving (parent + child table, `@Transactional`) |
| `dao` | `VehiclesDAO` (`@Repository`) | parent table: insert, update, delete, find by id / plate, **dynamic filtered select**, row mapping |
| | `CarDAO`, `BikeDAO`, `BicycleDAO` (`@Repository`) | child tables: insert and update |
| | `LookupDAO` (`@Repository`) | allowed values from the lookup tables |
| `models` | `Vehicles`, `Car`, `Bike`, `Bicycle` | data classes (Lombok, `@SuperBuilder`) |
| `enums` | `Operation` | allowed operations and the name of the method that runs each one |
| `exception` | `CustomException` | thrown for every invalid row |
| `utils` | `Validation` | required values, numbers, ranges, allowed values, booleans, plate format |
| | `FileManager` | read the input file, write the output file |

---

## Flow of one row

    VehiclesJdbCspringBootApplication
      └─ CommandLineRunner -> StartVehicle.execute()      clear writeData.txt, read readData.txt
           └─ processRows                                  for each row: parse, read the operation, catch CustomException
                └─ (reflection) ListImplementation.<method of the operation>
                     ├─ addVehicle      type from the row, check it in vehicle_types
                     │                  -> executeOperation("add", "car", parameters)
                     ├─ updateVehicle   id (or plate -> id) -> findById -> merge current + new values
                     │                  -> executeOperation("update", <type from DB>, merged)
                     ├─ deleteVehicle   id (or plate -> id) -> findById
                     │                  -> executeOperation("delete", <type from DB>, {id})
                     └─ listVehicles    validate filters -> VehiclesDAO.findByFilters -> writeData.txt

    executeOperation(operation, type, parameters)
      └─ context.getBean(type + "Implementation")         e.g. CarImplementation (Spring bean)
           └─ (reflection) <operation>(Map)                add / update / delete
                └─ build(): validation -> VehiclesDAO + CarDAO (one transaction)

Example: `update,plate=FL234GX,color=red`

    StartVehicle        UPDATE -> invoke ListImplementation.updateVehicle(String[])
    ListImplementation  plate FL234GX -> id 2 -> findById(2) = Car (white, cc=1300, ...)
                        merged = current values + {color=red} + {id=2}
                        getBean("carImplementation") -> invoke update(Map)
    CarImplementation   build(merged): validation -> vehiclesDAO.update + carDAO.update

---

## Database design

The database is in `script.sql`. It has three groups of tables.

### 1. Lookup tables

`vehicle_types`, `fuel_types`, `categories`, `colors`, `brand_types`.
Each one has an identity id and a unique `description` (e.g. `colors`: `white`, `black`, ...).

- They replace the `List.of(...)` constants of Project01: the allowed values are now
  data, not code. To add a color, insert a row: no Java change.
- `LookupDAO` reads them (`findColors()`, `findBrands()`, ...) and
  `VehicleAbstract.fillProperties` validates the input against them
  (`Validation.getOneOf`).
- `vehicles` stores only the **id** of each lookup value (foreign keys).
  The insert finds the id from the description with a sub-query, e.g.
  `(select id_color from colors where description = :color)`.

### 2. Parent table `vehicles`

Common attributes of every vehicle (the fields of the Java class `Vehicles`):
type, fuel, category, color, brand (foreign keys), model, number of wheels,
number of gears, production year. `id_vehicle` is an identity column.

### 3. Child tables `cars`, `bikes`, `bicycles`

Only the attributes specific to each type (the fields of `Car`, `Bike`, `Bicycle`).

- **Shared primary key**: `id_vehicle` is both the primary key of the child table
  and a foreign key to `vehicles`. So one vehicle has exactly one child row, with
  the same id.
- **`on delete cascade`**: deleting a row of `vehicles` also deletes its child row,
  so a delete needs one statement only.

This mapping of Java inheritance to tables (one table for the parent class, one
table per subclass, joined by the same id) is known as **Class Table Inheritance**
(or "table per subclass").

### Constraints

| Constraint | Where |
|------------|-------|
| `not null` | every required column |
| `unique` | lookup descriptions, `cars.plate`, `bikes.plate` |
| `check` ranges | wheels 2-8, gears 1-50, year 1976-2026, cc, doors |
| `check` plate format | `cars.plate ~ '^[A-Z]{2}[0-9]{3}[A-Z]{2}$'`, `bikes.plate ~ '^[A-Z]{2}[0-9]{5}$'` |
| foreign keys | lookup ids in `vehicles`, `id_vehicle` in the child tables |

The same rules are also checked in Java (`Validation`, `build`), so an invalid
row gets a clear message and is skipped. Some rules exist **only in Java** because
a simple constraint cannot express them:

- a plate must be unique across **cars and bikes together** (`unique` works on one table only);
- rules that depend on the type (car >= 3 wheels, bicycle only `manual` / `electric`).

The year check of the database (1976-2026) is fixed, while Java uses
"last 50 years": the script must be updated when the year changes.

### Reading a vehicle

`VehiclesDAO.SELECT_VEHICLES` joins `vehicles` with the five lookup tables
(`join`) and with the three child tables (`left join`, because only one of them
has a row for each vehicle). `mapVehicle` reads `vehicle_type` and creates the
right Java class:

| `vehicle_type` | object | filled columns |
|----------------|--------|----------------|
| `car` | `Car` | `car_plate`, `car_cc`, `number_doors` |
| `bike` | `Bike` | `bike_plate`, `bike_cc` |
| `bicycle` | `Bicycle` | `suspension`, `foldable` |

---

## DAO pattern

A **DAO (Data Access Object)** is a class that contains all the SQL of one table
(or one concept). The rest of the program calls Java methods (`insert`, `update`,
`findById`) and never writes SQL. In this project:

| DAO | Table(s) | Methods |
|-----|----------|---------|
| `VehiclesDAO` | `vehicles` (+ joins for reading) | `insert`, `update`, `delete`, `findById`, `findIdByPlate`, `existsByPlate`, `findByFilters` |
| `CarDAO` | `cars` | `insert`, `update` |
| `BikeDAO` | `bikes` | `insert`, `update` |
| `BicycleDAO` | `bicycles` | `insert`, `update` |
| `LookupDAO` | lookup tables | `findVehicleTypes`, `findFuelTypes`, `findCategories`, `findColors`, `findBrands` |

Common points (same style as `ClienteDAO` in L016_sqljdbc):

- `@Repository` + `@RequiredArgsConstructor` + `private final NamedParameterJdbcTemplate jdbcTemplate`.
- **Named parameters**: the SQL contains `:name`, the values are passed with
  `MapSqlParameterSource.addValue("name", value)`. Every `:name` in the SQL needs
  its own `addValue`, in every statement.
- **Insert with generated key**: `VehiclesDAO.insert` uses a `GeneratedKeyHolder`
  and returns the new `id_vehicle`; the child DAOs receive it through `car.getId()`.
- **`Optional`** for single results that may be missing (`findById`, `findIdByPlate`):
  the service decides what to do with `orElseThrow(...)`.
- **Update / delete** return the number of rows changed: `0` means "not found".
- **Mapping a row to an object** is written by hand in `mapVehicle` (no reflection).

### Transactions

A car is stored in two tables (`vehicles` + `cars`), so saving it means two SQL
statements. `add`, `update` and `delete` of the implementations are
`@Transactional`: if the second statement fails (or a check throws an exception),
the first one is rolled back, so the database never contains half a vehicle.

`@Transactional` works through a Spring proxy: it is applied only when the method
is called **from another bean** (here `ListImplementation` calls the
implementations), not from a method of the same class.

---

## Dynamic query (`list`)

`list` alone prints all vehicles; `list` followed by `key=value` filters prints
only the vehicles that match **all** of them:

    list
    list,color=white
    list,type=car,color=white,brand=fiat
    list,suspension=no

1. **Validation in the service** (`ListImplementation.validateFilters`): each filter
   name must exist in `VehiclesDAO.FILTERS`, and its value must have the right
   format (number for `I`, yes/no for `B`, not empty for `S`). Otherwise:
   "Unknown filter: speed" / "Parameter year must be a number".
2. **SQL built in the DAO** (`VehiclesDAO.findByFilters`), with two maps:

       FILTER_COLUMNS   filter name -> column of SELECT_VEHICLES     (e.g. color -> co.description)
       FILTERS          filter name -> type of value                 (S = text, I = integer, B = boolean)

   The query starts with `where 1 = 1` and adds one condition per filter:

       S:  and lower(<column>) = lower(:<name>)      (text, case-insensitive)
       I:  and <column> = :<name>                    (value converted with Integer.valueOf)
       B:  and <column> = :<name>                    (yes / true -> true)

   Example: `list,type=car,color=white` produces

       select ... from vehicles v join ... left join ...
        where 1 = 1
          and lower(vt.description) = lower(:type)
          and lower(co.description) = lower(:color)
        order by v.id_vehicle

   `plate` and `cc` exist in two tables, so their column is
   `coalesce(c.plate, b.plate)` / `coalesce(c.cc, b.cc)`.
   `where 1 = 1` is always true: it only lets every filter start with `and`,
   and with no filters the query returns all vehicles.
3. **Result**: a `List<Vehicles>` (`Car`, `Bike`, `Bicycle` objects), converted with
   `map(Vehicles::toString)` and appended to `writeData.txt`:

       ### list,type=car,color=white (2 found) ###
       Car(super=Vehicles(id=2, vehicleType=car, ...), plate=FL234GX, cc=1300, numberDoors=4)

The generated SQL is printed in the log (`Query: ...`).

### Why this is safe from SQL injection

- The **columns** come only from `FILTER_COLUMNS`, fixed in the code.
- The **values** are never written into the SQL: they are `:parameters`, sent to
  the database separately (prepared statement), so they are treated as data.
- The **filter name** is used as parameter name (`:color`) only after it has been
  checked against the maps: an unknown name is rejected before reaching the SQL.

To add a filter: one line in `FILTER_COLUMNS` and one in `FILTERS`.

---

## Update

An `update` row contains only the fields to change, but `build()` validates
**all** the fields of a vehicle. `ListImplementation.updateVehicle` therefore:

1. finds the id (`id=...`, or `plate=...` -> `VehiclesDAO.findIdByPlate`);
2. reads the current vehicle (`VehiclesDAO.findById`);
3. converts it into a map with the same keys of the input file (`toParameters`):
   `{wheels=4, fuel=petrol, color=white, plate=FL234GX, cc=1300, doors=4, ...}`;
4. overwrites it with the new values (`merged.putAll(newValues)`) and adds the id;
5. calls `update` of the right implementation: `build(merged)` validates everything
   again, then `VehiclesDAO.update` + child DAO `update` (one transaction).

The plate check in `update` accepts the vehicle's own plate and rejects a plate
that belongs to another vehicle. To change a plate, find the vehicle by `id`.

---

## Spring: beans and dependency injection

- `@SpringBootApplication` scans `com.betacom.vehicles.*` and creates one object
  (**bean**) for every class marked `@Component` or `@Repository`.
- Dependencies are declared as `private final` fields; Lombok
  `@RequiredArgsConstructor` generates the constructor and Spring calls it with
  the right beans (**constructor injection**). No class uses `new` to create
  services or DAOs.
- Beans are **singletons by default**: one `VehiclesDAO`, one `ListImplementation`, ...
  shared by the whole application. This is why the `VehicleRegistry` of Project01
  singleton is not needed anymore.
- `VehicleAbstract` is abstract, so its `LookupDAO` is injected with `@Autowired`
  on the field (Spring fills it when it creates each implementation).
- `NamedParameterJdbcTemplate` is created by Spring Boot from the datasource in
  `application.properties`.

---

## Reflection and dynamic call

Reflection finds a method **by its name at runtime** (`getMethod` + `invoke`),
instead of a `switch` written in the code. It is used at two levels, both on
Spring beans (objects are never created with reflection: Spring creates them,
otherwise their injected DAOs would be `null`).

### Level 1: operation -> method of `ListImplementation` (`StartVehicle`)

Each `Operation` stores the name of the method that runs it:

    ADD("addVehicle"), UPDATE("updateVehicle"), DELETE("deleteVehicle"), LIST("listVehicles")

`StartVehicle.handleOperation`:

    Method method = service.getClass().getMethod(operation.getMethodName(), String[].class);
    method.invoke(service, (Object) parametersList);      // (Object): invoke takes Object... varargs

Every operation method has the same parameter (`String[]`, the whole row), so one
lookup works for all of them.

### Level 2: vehicle type -> Spring bean -> method (`ListImplementation.executeOperation`)

Same idea as `StartVeicoli.executeOperation` in the supervisor's project:

    String componentName = vehicleType + "Implementation";           // "car" -> "carImplementation"
    if (!context.containsBean(componentName))
        throw new CustomException("Vehicle type " + vehicleType + " not supported");

    VehicleOperations component = context.getBean(componentName, VehicleOperations.class);
    Method method = component.getClass().getMethod(operation, Map.class);   // "add", "update", "delete"
    method.invoke(component, parameters);

- **Bean name convention**: a class annotated `@Component` without a name gets the
  class name with a lowercase first letter (`CarImplementation` -> `carImplementation`).
- **`ApplicationContext`** is the Spring container: `containsBean` checks that the
  bean exists, `getBean` returns it.
- **`VehicleOperations`**: all implementations have `add(Map)`, `update(Map)`,
  `delete(Map)` with the same signature, so `getMethod(operation, Map.class)` works
  for every type and every operation.
- `getBean` returns the Spring **proxy** of the implementation, so the method found
  by `getMethod` is the proxy's one and `@Transactional` is still applied.
- A new vehicle type needs a new `XxxImplementation extends VehicleAbstract`
  (`@Component`), its model, its child table and DAO, and a row in `vehicle_types`:
  `ListImplementation` and `StartVehicle` do not change.

### Reflection exceptions

| Exception | When |
|-----------|------|
| `NoSuchMethodException` | no public method with that name and parameter types |
| `IllegalAccessException` | the method exists but is not accessible |
| `InvocationTargetException` | the called method threw an exception: the real one is in `getCause()` |

All of them are converted into `CustomException`, so the row is logged and the
program continues with the next one.

### Pros and cons

- Pro: no `switch` to update when an operation or a vehicle type is added
  (open/closed principle).
- Con: a wrong name is found only at runtime ("Operation not found"), not by the
  compiler. Without the exercise requirement, a `switch` and an injected
  `Map<String, VehicleAbstract>` would do the same job.

---

## Builder pattern (`@SuperBuilder`)

The **builder pattern** creates an object by naming each field, instead of a long
constructor or many setters:

    Car car = Car.builder()
                 .plate("FL234GX")
                 .cc(1300)
                 .numberDoors(4)
                 .build();

The models use inheritance (`Car extends Vehicles`), and Lombok's `@Builder` does
not include the fields of the parent class. **`@SuperBuilder`** (on `Vehicles`
and on every subclass) creates builders that know the fields of the whole
hierarchy (`Car.builder().id(...).brand(...).plate(...)` is valid).

Where it is used: `VehiclesDAO.mapVehicle`, to create the object from a database
row (type-specific fields with the builder, common fields with the setters):

    Vehicles vehicle = switch (type) {
        case "car"     -> Car.builder().plate(...).cc(...).numberDoors(...).build();
        case "bike"    -> Bike.builder().plate(...).cc(...).build();
        case "bicycle" -> Bicycle.builder().suspensions(...).foldable(...).build();
        default        -> throw new CustomException("Unknown vehicle type in database: " + type);
    };

Where it is not used: `build()` of the implementations keeps `new Car()` + setters,
because the object is filled step by step during validation
(`fillProperties`, then each specific field).

Annotations on the models:

| Annotation | Why |
|------------|-----|
| `@Getter`, `@Setter` | read and write the fields (setters are still used for `setId` after the insert) |
| `@ToString(callSuper = true)` | readable text for `writeData.txt` and the log; `callSuper` adds the `Vehicles` part |
| `@SuperBuilder` | builder that works with inheritance |
| `@NoArgsConstructor` | `new Car()` in the implementations |

---

## Other design patterns and principles

| Pattern / principle | Where |
|---------------------|-------|
| Layered architecture | `process` (input and dispatch) -> `services` (rules) -> `dao` (SQL) -> database |
| Dependency injection / Inversion of control | Spring creates the objects and passes them to the constructors |
| DAO | one class with the SQL of each table |
| Class Table Inheritance | parent table `vehicles` + one child table per subclass |
| Strategy (via interface + polymorphism) | `VehicleOperations`: the same operation, a different implementation for each vehicle type, chosen at runtime |
| Template-like base class | `VehicleAbstract.fillProperties` validates the common part, each implementation adds its own rules in `build` |
| Builder | `@SuperBuilder` in `mapVehicle` |
| Transaction (unit of work) | `@Transactional` on add / update / delete |
| Fail fast validation | rows are validated in Java before any SQL; the database constraints are a second safety net |

---

## Validation and error handling

- **Parsing** (`StringParsing`): every parameter must be `key=value`; duplicate keys
  are rejected.
- **Common attributes** (`VehicleAbstract.fillProperties`): required values,
  numbers, year range, values from the lookup tables.
- **Type-specific rules** (`build` of each implementation): plate format, cc,
  doors, gears ranges, wheels and fuel per type.
- **Plate uniqueness** (`VehiclesDAO.existsByPlate` / `findIdByPlate`): across cars
  and bikes.

Every invalid row throws a `CustomException`. `StartVehicle.processRows` catches it,
logs `Row N ... <reason>` and continues with the next row. Reflection errors are
converted into `CustomException` too.

Errors coming from the database (lost connection, violated constraint not checked
in Java) are Spring `DataAccessException`s: when they happen inside a reflective
call they are also converted into `CustomException` by the `InvocationTargetException`
handler, so the row is skipped.

---

## Stream API and lambdas

- `FileManager.readFile`: `reader.lines().filter(...).toList()` drops blank and comment lines
- `StringParsing.rowParser`: `Arrays.stream(...).map(String::trim).toArray(String[]::new)`
- `StringParsing.mapKeyValue`: `stream().map(splitKeyValue).collect(Collectors.toMap(...))`
- `ListImplementation`: `Arrays.stream(...).skip(n)` to drop command / type,
  `findByFilters(...).stream().map(Vehicles::toString).toList()` for `list`
- `Optional.orElseThrow(...)` for "not found" cases
- method references: `Vehicles::toString`, `String::trim`, `StringParsing::splitKeyValue`

---

## Logging

Configured in `application.properties`:

- `logging.level.root=info`, `logging.level.com.betacom=debug`
  (debug messages of this project are shown, e.g. the generated SQL);
- custom console pattern: date, level, thread, method, file and line.

Classes that log use Lombok `@Slf4j`. Levels: `error` for rejected rows,
`info` for added / updated / deleted vehicles and written records, `debug` for details.
