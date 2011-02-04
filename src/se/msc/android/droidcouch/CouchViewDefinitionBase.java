package se.msc.android.droidcouch;

public abstract class CouchViewDefinitionBase {

	public CouchDesignDocument Doc;
    public String Name;

    protected CouchViewDefinitionBase(String name, CouchDesignDocument doc)
    {
        Doc = doc;
        Name = name;
    }

    public CouchDatabase Db()
    {
        return Doc.Owner;
    }

    public CouchRequest Request()
    {
        return Db().Request(Path());
    }

    public String Path()
    {
        if (Doc.Id == "_design/")
        {
            return Name;
        }
        return Doc.Id + "/_view/" + Name;
    }
}
