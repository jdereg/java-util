package com.cedarsoftware.ncube;

import com.cedarsoftware.util.ProxyFactory;
import com.lordofthejars.nosqlunit.annotation.ShouldMatchDataSet;
import com.lordofthejars.nosqlunit.annotation.UsingDataSet;
import com.lordofthejars.nosqlunit.core.LoadStrategyEnum;
import com.lordofthejars.nosqlunit.mongodb.EmbeddedMongoInstancesFactory;
import com.lordofthejars.nosqlunit.mongodb.InMemoryMongoDb;
import com.lordofthejars.nosqlunit.mongodb.MongoDbRule;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import java.lang.reflect.InvocationHandler;

import static com.lordofthejars.nosqlunit.mongodb.InMemoryMongoDb.InMemoryMongoRuleBuilder.newInMemoryMongoDbRule;
import static com.lordofthejars.nosqlunit.mongodb.MongoDbRule.MongoDbRuleBuilder.newMongoDbRule;
import static org.junit.Assert.assertEquals;

/**
 * Created by ken on 8/21/2014.
 */

public class TestMongoServiceInvocationHandler
{
    @ClassRule
    public static InMemoryMongoDb inMemoryMongoDb = newInMemoryMongoDbRule().build();

    @Rule
    public MongoDbRule embeddedMongoDbRule = newMongoDbRule().defaultEmbeddedMongoDb("test");

    //@After
    //public void tearDown() {
        //Mongo defaultEmbeddedInstance = EmbeddedMongoInstancesFactory.getInstance().getDefaultEmbeddedInstance();
        //defaultEmbeddedInstance.getDB("test").getCollection("collection1").drop();
    //}


    private Mongo getDataSource() {
        return EmbeddedMongoInstancesFactory.getInstance().getDefaultEmbeddedInstance();
    }

    @Test
    @UsingDataSet(locations="testAdapter-initial.json", loadStrategy= LoadStrategyEnum.CLEAN_INSERT)
    @ShouldMatchDataSet(location="testAdapter-expected.json")
    public void testSaveWithNewItem() {
        InvocationHandler h = new MongoServiceInvocationHandler(getDataSource(), FooService.class, new MongoFooService());
        FooService service = ProxyFactory.create(FooService.class, h);

        //finish tests later.
        assertEquals("John D.", service.getFoo(1));

        String name = service.getFoo(2);
        assertEquals("Kenny P.", name);

        //  Will add another item in since we aren't passing in _id with a value set.
        service.saveFoo(2, "Chuck R.");
    }

    private interface FooService {
        public String getFoo(int fooId);
        public boolean saveFoo(int fooId, String name);
    }

    private class MongoFooService {
        public String getFoo(Mongo mongo, int fooId) {
            DBCollection collection = mongo.getDB("test").getCollection("foo");
            BasicDBObject query = new BasicDBObject("id", fooId);
            DBObject res = collection.findOne(query);
            return (String)res.get("code");
        }

        public boolean saveFoo(Mongo mongo, int fooId, String name) {
            DBCollection collection = mongo.getDB("test").getCollection("foo");

            BasicDBObject doc = new BasicDBObject("id", fooId)
                    .append("code", name);
            collection.insert(doc);
            return true;
        }
    }


}
