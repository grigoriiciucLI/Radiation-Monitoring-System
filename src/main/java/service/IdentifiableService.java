package service;
import model.Entity;
import repository.Repository;
import validator.Validator;
import java.sql.SQLException;
import java.util.List;
/**
 * Abstract generic service that handles validation and delegates to a repository.
 * @param <ID> primary key type
 * @param <E>  entity type
 * @param <R>  repository type — allows passing any subinterface (StationRepository, ReadingRepository)
 */
public abstract class IdentifiableService<ID, E extends Entity<ID>, R extends Repository<ID, E>> {

    private final R repository;
    private final Validator<E> validator;

    protected IdentifiableService(R repository, Validator<E> validator) {
        this.repository = repository;
        this.validator  = validator;
    }

    public List<E> findAll() throws SQLException {
        return repository.findAll();
    }

    public E findById(ID id) throws SQLException {
        if (id == null) throw new ServiceException("ID cannot be null.");
        return repository.findById(id);
    }

    public ID save(E entity) throws SQLException {
        if (entity == null) throw new ServiceException("Entity cannot be null.");
        validator.validate(entity);
        return repository.save(entity);
    }

    public void update(E entity) throws SQLException {
        if (entity == null) throw new ServiceException("Entity cannot be null.");
        validator.validate(entity);
        repository.update(entity);
    }

    public void delete(ID id) throws SQLException {
        if (id == null) throw new ServiceException("ID cannot be null.");
        repository.delete(id);
    }

    /** Exposes the repository to subclasses for extra queries (search, findByStation, etc.) */
    protected R getRepository() { return repository; }
}