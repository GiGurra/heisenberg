package se.gigurra.heisenberg

trait Component {
  protected def parse[FieldType](field: FieldOption[FieldType], orElse: => Option[FieldType]): Option[FieldType]
  protected def parse[FieldType](field: FieldOption[FieldType]): Option[FieldType]

  protected def parse[FieldType](field: FieldRequired[FieldType], orElse: => FieldType): FieldType
  protected def parse[FieldType](field: FieldRequired[FieldType]): FieldType
}
