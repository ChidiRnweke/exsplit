package exsplit.datamapper

import exsplit.spec.NotFoundError

/** Represents a generic data mapper that provides CRUD operations for a
  * specific entity.
  *
  * @tparam F
  *   The effect type, representing the context in which the operations are
  *   executed.
  * @tparam A
  *   The input type for the create operation.This is the type of the data that
  *   is used to create a new entity. Typically, this is the type received from
  *   smithy4s' codegen.
  * @tparam B
  *   The read mapper type for the entity. This is a one-to-one mapping of the
  *   entity in the database without the creation and update timestamps.
  * @tparam C
  *   The input type for the update operation. This is the type of the data that
  *   is used to update an existing entity. This type is obtained by making a
  *   change to the domain model. If it contains more than one field that can be
  *   updated independently, all fields except the ID should be optional.
  */
trait DataMapper[F[_], A, B, C]:
  /** Creates a new entity.
    *
    * @param input
    *   The input data for creating the entity.
    * @return
    *   The created entity wrapped in the effect type F.
    */
  def create(input: A): F[B]

  /** Retrieves an entity by its ID.
    *
    * @param id
    *   The ID of the entity to retrieve.
    * @return
    *   Either the retrieved entity or a NotFoundError wrapped in the effect
    *   type F.
    */
  def get(id: String): F[Either[NotFoundError, B]]

  /** Updates an existing entity. Does not return an error if the entity does
    * not exist such that the operation is idempotent.
    *
    * @param b
    *   The updated data for the entity.
    * @return
    *   The updated entity wrapped in the effect type F.
    */
  def update(b: C): F[Unit]

  /** Deletes an entity by its ID. Does not return an error if the entity does
    * not exist such that the operation is idempotent.
    *
    * @param id
    *   The ID of the entity to delete.
    * @return
    *   Unit wrapped in the effect type F.
    */
  def delete(id: String): F[Unit]

/** Represents a relationship where a parent entity has many child entities.
  * This is a one-to-many relationship. The parent entity is the entity that is
  * the foreign key in the child entity.
  *
  * @tparam F
  *   The effect type, representing the context in which the operations are
  *   executed.
  * @tparam P
  *   The type of the parent entity. This may be the ID of the parent entity or
  *   the parent entity itself.
  * @tparam C
  *   The type of the child entities.
  */
trait HasMany[F[_], P, C]:
  /** Lists all the child entities of a given parent entity.
    *
    * @param parent
    *   The parent entity or the ID of the parent entity.
    * @return
    *   A list of child entities wrapped in the effect type F.
    */
  def listChildren(parent: P): F[List[C]]

/** Represents a relationship where an entity belongs to another entity through
  * a third entity. This is a many-to-many relationship. The primary entity is
  * the entity that is the foreign key in the target entity. The target entity
  * is the entity that is the foreign key in the related entity.
  *
  * You must implement the inverse relationship as well. For example, if you
  * implement a `BelongsToThrough` relationship for `A` and `B` with a through
  * entity `C`, you must also implement a `BelongsToThrough` relationship for
  * `B` and `A` with the same through entity `C`.
  *
  * **Note:** the through entity is not used in the operations. It is only used
  * to represent the relationship. Technically, the through entity is not needed
  * for the operations so it could be removed from the type signature.
  *
  * @tparam F
  *   The effect type, representing the context in which the operations are
  *   executed.
  * @tparam P
  *   The type of the primary entity.
  * @tparam T
  *   The type of the through entity.
  * @tparam R
  *   The read mapper type for the related entity. This is a one-to-one mapping
  *   of the related entity in the database without the creation and update
  *   timestamps.
  */
trait BelongsToThrough[F[_], P, T, R]:
  /** Lists all the related entities of a given primary entity.
    *
    * @param entityId
    *   The ID of the primary entity.
    * @return
    *   A list of related entities wrapped in the effect type F.
    */
  def listPrimaries(entityId: P): F[List[R]]
