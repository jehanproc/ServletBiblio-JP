package service.impl;

import java.util.Date;

import service.Bibliotheque;
import dao.AdherentDao;
import dao.EmpruntDao;
import dao.LivreDao;
import dao.jdbc.LivreDaoJdbc;
import dao.jpa.LivreDaoJpa;
import dao.memory.AdherentDaoMemory;
import dao.memory.EmpruntDaoMemory;
import dao.memory.LivreDaoMemory;
import entity.Adherent;
import entity.BusinessException;
import entity.Emprunt;
import entity.Livre;

public class BibliothequeImpl implements Bibliotheque {
	final int maxLivreIdentique;
	final int maxEmpruntAdherent;
	
	LivreDao livreDao = new LivreDaoJpa();
	AdherentDao adherentDao = new AdherentDaoMemory();
	EmpruntDao empruntDao = new EmpruntDaoMemory();
	
	public BibliothequeImpl(Integer maxLivreIdentique, Integer maxEmpruntAdherent) {
		this.maxLivreIdentique = maxLivreIdentique; 
		this.maxEmpruntAdherent = maxEmpruntAdherent;
	}

	@Override
	public int getMaxEmpruntAdherent() { 
		return maxEmpruntAdherent;
	}
	
	@Override
	public int getMaxLivreIdentique() {
		return maxLivreIdentique;
	}
	
	@Override
	public int ajouterLivre(Livre livre)  {
		//RG : maxLivreIdentique ?
		if(livreDao.getCount(livre) == maxLivreIdentique ) 
			throw new BusinessException("BibliothequeImpl.ajouterLivre", null);
		livreDao.save(livre);
		return livre.getId();
	}


	@Override
	public void retirerLivre(int idLivre) {
		//RG : livre vacant ?
		if(empruntDao.getEmpruntEnCoursByLivre(idLivre) != null) 
			throw new BusinessException("BibliothequeImpl.retirerLivre", null); 
		//détruire d'abord les anciens emprunts puis le live ....
		empruntDao.delete(empruntDao.getAllByLivre(idLivre));
		livreDao.delete(idLivre);
	}

	@Override
	public int ajouterAdherent(Adherent adherent) {
		//RG est déjà Present ?
		if(adherentDao.isPresent(adherent))
			throw new BusinessException("BibliothequeImpl.ajouterAdherent", null); 
		adherentDao.save(adherent);
		return adherent.getId();
	}


	@Override
	public void retirerAdherent(int idAdherent) {
		//RG : adherent vacant ?
		if(empruntDao.getEmpruntsEnCoursByAdherent(idAdherent).size() > 0)
			throw new BusinessException("BibliothequeImpl.retirerAdherent", null); 
		//détruire d'abord les anciens emprunts puis l'adhérent ....
		empruntDao.delete(empruntDao.getAllByAdherent(idAdherent));
		adherentDao.delete(idAdherent);	
	}

	@Override
	public void emprunterLivre(int idLivre, int idAdherent) {
		//RG : maxEmpruntAdherent ?
		if( empruntDao.getAllByAdherent(idAdherent).size() == maxEmpruntAdherent)
			throw new BusinessException("BibliothequeImpl.emprunterLivre", null); 
		//RG : livre déjà emprunté !
		if(empruntDao.getEmpruntEnCoursByLivre(idLivre) != null)
			throw new BusinessException("BibliothequeImpl.emprunterLivre", null); 

		empruntDao.save(new Emprunt(livreDao.findOne(idLivre), adherentDao.findOne(idAdherent)));
	}
	
	@Override
	public void restituerLivre(int idLivre, int idAdherent) {
		// RG : un emprunt doit exist� avec le couple idLivre/idAdherent
		Emprunt emprunt = empruntDao.getEmpruntEnCoursByLivre(idLivre);
		if(emprunt == null || emprunt.getAdherent().getId() != idAdherent)
			throw new BusinessException("BibliothequeImpl.restituerLivre", null);  /// A finir ...
		emprunt.setFin(new Date());
		empruntDao.update(emprunt);
		
	}

	@Override
	public void transfererEmprunt(int idAdherentPrecedent, int idLivre,
			int idAdherentSuivant) {
		restituerLivre(idLivre, idAdherentPrecedent);
		emprunterLivre(idLivre, idAdherentSuivant);	
	}

}
