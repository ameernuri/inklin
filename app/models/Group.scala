package models

import java.util.UUID._

import org.anormcypher._
import org.anormcypher.CypherParser._
import monkeys.DoLog._

case class Group(uuid: String, name: String, description: String, admin: String)

object Group {

	private def log(log: String, params: Map[String, Any] = Map()) = modelLogger("Group", log, params)

	val simple = {
		get[String]("group.uuid") ~
		get[String]("group.name") ~
		get[String]("group.description") ~
		get[String]("admin.uuid")  map {
			case uuid~name~description~admin =>
				Group(uuid, name, description, admin)
		}
	}

	def simpleReturn(group: String = "group"): String = {
		s"""
		  |$group.uuid, $group.name, $group.description, admin.uuid
		""".stripMargin
	}

	def create(name: String, description: String, admin: String): Option[Group] = {
		log("create", Map(
			"name" -> name,
			"description" -> description,
			"admin" -> admin
		))

		Cypher(
			s"""
				|MATCH (admin:User {uuid: {admin}})
			  |CREATE (admin)-[:is_group_admin]->(group:Group {
        | uuid: "$randomUUID",
			  | name: {name},
			  | description: {description},
			  | created: timestamp()
			  |})
			  |RETURN ${simpleReturn()}
			""".stripMargin
		).on(
			"name" -> name,
			"description" -> description,
			"admin" -> admin
		).as(simple.singleOpt)
	}

	def find(uuid: String): Option[Group] = {
		log("find", Map("uuid" -> uuid))

		Cypher(
			s"""
			  |MATCH (admin:User)-[:is_group_admin]->(group:Group {uuid: {uuid}})
			  |RETURN ${simpleReturn()}
			""".stripMargin
		).on(
			"uuid" -> uuid
		).as(simple.singleOpt)
	}

	def findOwned(user: String, limit: Int = 10): Seq[Group] = {
		log("find", Map("user" -> user, "limit" -> limit))

		Cypher(
			s"""
			  |MATCH (admin:User {uuid: {user}})-[:is_group_admin]->(group:Group)
			  |RETURN ${simpleReturn()}
				|LIMIT {limit}
			""".stripMargin
		).on(
			"user" -> user,
			"limit" -> limit
		).as(simple.*)
	}

	def ownsAny(user: String): Boolean = {
		log("find", Map("user" -> user))

		Cypher(
			s"""
			  |MATCH (admin:User {uuid: {user}})-[:is_group_admin]->(group:Group)
			  |RETURN count(group)
			""".stripMargin
		).on(
			"user" -> user
		).as(scalar[Int].single) > 0
	}

	def exists(uuid: String): Boolean = {
		log("exists", Map("uuid" -> uuid))

		Cypher(
			"""
			  |MATCH (group:Group {uuid: {uuid}})
			  |RETURN count(group)
			""".stripMargin
		).on(
			"uuid" -> uuid
		).as(scalar[Int].single) > 0
	}

	def isMember(user: String, group: String): Boolean = {
		log("isMember", Map("user" -> user, "group" -> group))

		Cypher(
			"""
			  |MATCH (user:User {uuid: {user}})-[m:is_group_member]->(group:Group {uuid: {group}})
			  |RETURN count(m)
			""".stripMargin
		).on(
			"user" -> user,
			"group" -> group
		).as(scalar[Int].single) > 0
	}

	def isAdmin(user: String, group: String): Boolean = {
		log("isAdmin", Map("user" -> user, "group" -> group))

		Cypher(
			"""
			  |MATCH (user:User {uuid: {user}})-[m:is_group_admin]->(group:Group {uuid: {group}})
			  |RETURN count(m)
			""".stripMargin
		).on(
			"user" -> user,
			"group" -> group
		).as(scalar[Int].single) > 0
	}
}
