package se.gigurra.heisenberg

trait Component {
  protected def parse[FieldType : MapDataProducer](field: FieldOption[FieldType], orElse: => Option[FieldType]): Option[FieldType]
  protected def parse[FieldType : MapDataProducer](field: FieldOption[FieldType]): Option[FieldType]

  protected def parse[FieldType : MapDataProducer](field: FieldRequired[FieldType], orElse: => FieldType): FieldType
  protected def parse[FieldType : MapDataProducer](field: FieldRequired[FieldType]): FieldType
}
