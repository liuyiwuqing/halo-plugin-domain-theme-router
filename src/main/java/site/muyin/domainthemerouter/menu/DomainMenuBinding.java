package site.muyin.domainthemerouter.menu;

/** A menu selected by a successfully applied domain theme route, scoped to one request. */
public record DomainMenuBinding(String menuName) {

    public static final String ATTRIBUTE = DomainMenuBinding.class.getName();
}
