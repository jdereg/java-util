package com.cedarsoftware.ncube;

import com.cedarsoftware.util.ProxyFactory;
import com.lordofthejars.nosqlunit.mongodb.EmbeddedMongoInstancesFactory;
import com.lordofthejars.nosqlunit.mongodb.InMemoryMongoDb;
import com.lordofthejars.nosqlunit.mongodb.MongoDbRule;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import com.mongodb.MongoClient;
import com.mongodb.WriteResult;
import org.junit.After;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import java.lang.reflect.InvocationHandler;

import static com.lordofthejars.nosqlunit.mongodb.InMemoryMongoDb.InMemoryMongoRuleBuilder.newInMemoryMongoDbRule;
import static com.lordofthejars.nosqlunit.mongodb.MongoDbRule.MongoDbRuleBuilder.newMongoDbRule;

/**
 * Created by ken on 8/21/2014.
 */

public class TestMongoServiceInvocationHandler
{
    @ClassRule
    public static InMemoryMongoDb inMemoryMongoDb = newInMemoryMongoDbRule().build();

    @Rule
    public MongoDbRule embeddedMongoDbRule = newMongoDbRule().defaultEmbeddedMongoDb("test");

    MongoClient _client;

    @After
    public void tearDown() {
        //Mongo defaultEmbeddedInstance = EmbeddedMongoInstancesFactory.getInstance().getDefaultEmbeddedInstance();
        //defaultEmbeddedInstance.getDB("test").getCollection("collection1").drop();
    }


    //    private static DB database;
//
//    static{
//        try {
//            MongoClient mongo=new MongoClient("localhost",27017);
//            database=mongo.getDB("test");
//        } catch (UnknownHostException ex) {
//            throw new IllegalArgumentException(ex);
//        } catch (MongoException ex) {
//            throw new IllegalArgumentException(ex);
//        }
//    }


//    @Test
//    @UsingDataSet(locations="initialData.json", loadStrategy=LoadStrategyEnum.CLEAN_INSERT)
//    @ShouldMatchDataSet(location="expectedData.json")
//    public void book_should_be_inserted_into_repository() {
//
//
//        BookManager bookManager = new BookManager(MongoDbUtil.getCollection(Book.class.getSimpleName()));
//
//        Book book = new Book("The Lord Of The Rings", 1299);
//        bookManager.create(book);
//    }
    private Mongo getDataSource() {
        return EmbeddedMongoInstancesFactory.getInstance().getDefaultEmbeddedInstance();
    }

    @Test
    public void testAdapter() {
        InvocationHandler h = new MongoServiceInvocationHandler(getDataSource(), FooService.class, new MongoFooService());
        FooService service = ProxyFactory.create(FooService.class, h);

        //finish tests later.
        //assertEquals(null, service.getFoo(1));
        //assertTrue(service.saveFoo(1, "baz"));
        //assertEquals("baz", service.getFoo(1));

    }

    private interface FooService {
        public String getFoo(int fooId);
        public boolean saveFoo(int fooId, String name);
    }

    private class MongoFooService {
        public String getFoo(Mongo mongo, int fooId) {
            DBCollection collection = mongo.getDB("test").getCollection("ncube");
            DBObject object = collection.findOne();
            return (String)object.get("name");
        }

        public boolean saveFoo(Mongo mongo, int fooId, String name) {
            DBCollection collection = mongo.getDB("test").getCollection("ncube");

            BasicDBObject doc = new BasicDBObject("id", fooId)
                    .append("name", name);
            WriteResult wr = collection.insert(doc);
            return wr.getLastConcern() == null;
        }
    }


}
